package com.xiaohua.novel.agent.infrastructure;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohua.novel.agent.domain.AgentRunSnapshot;
import com.xiaohua.novel.agent.domain.AgentRunState;
import com.xiaohua.novel.agent.domain.AgentStepType;
import com.xiaohua.novel.agent.domain.ChapterDraft;
import com.xiaohua.novel.agent.domain.ChapterPlanDraft;
import com.xiaohua.novel.agent.domain.ChapterPlanRecord;
import com.xiaohua.novel.agent.domain.ChapterRecord;
import com.xiaohua.novel.agent.domain.CreatedRun;
import com.xiaohua.novel.agent.domain.CreativeProposalDraft;
import com.xiaohua.novel.agent.domain.CreativeProposalRecord;
import com.xiaohua.novel.agent.domain.DecisionRecord;
import com.xiaohua.novel.agent.domain.FoundationDraft;
import com.xiaohua.novel.agent.domain.ReviewReport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class NovelAgentRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public NovelAgentRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CreatedRun createRun(
            String idea,
            String genre,
            int targetChapters,
            long tokenBudget,
            BigDecimal costBudget,
            int timeBudgetSeconds) {
        String projectId = uuid();
        String runId = uuid();

        jdbcTemplate.update(
                """
                INSERT INTO novel_project (
                    id, original_idea, genre, status
                ) VALUES (?, ?, ?, ?)
                """,
                projectId,
                idea,
                genre,
                "DRAFT");
        jdbcTemplate.update(
                """
                INSERT INTO novel_agent_run (
                    id, project_id, state, current_chapter_no, target_chapter_no,
                    token_budget, cost_budget, time_budget_seconds, started_at
                ) VALUES (?, ?, ?, 0, ?, ?, ?, ?, CURRENT_TIMESTAMP(6))
                """,
                runId,
                projectId,
                AgentRunState.CREATED.name(),
                targetChapters,
                tokenBudget,
                costBudget,
                timeBudgetSeconds);
        return new CreatedRun(runId, projectId);
    }

    public Optional<AgentRunSnapshot> findRun(String runId) {
        List<AgentRunSnapshot> result = jdbcTemplate.query(
                """
                SELECT
                    r.id AS run_id,
                    r.project_id,
                    p.original_idea,
                    p.genre,
                    p.title,
                    p.status AS project_status,
                    r.state,
                    r.current_step,
                    r.current_chapter_no,
                    r.target_chapter_no,
                    r.token_budget,
                    r.token_used,
                    r.cost_budget,
                    r.cost_used,
                    r.time_budget_seconds,
                    r.last_checkpoint,
                    r.started_at,
                    r.completed_at,
                    r.updated_at
                FROM novel_agent_run r
                JOIN novel_project p ON p.id = r.project_id
                WHERE r.id = ?
                """,
                this::mapRun,
                runId);
        return result.stream().findFirst();
    }

    public List<AgentRunSnapshot> findInterruptedChapterRuns() {
        return jdbcTemplate.query(
                """
                SELECT
                    r.id AS run_id,
                    r.project_id,
                    p.original_idea,
                    p.genre,
                    p.title,
                    p.status AS project_status,
                    r.state,
                    r.current_step,
                    r.current_chapter_no,
                    r.target_chapter_no,
                    r.token_budget,
                    r.token_used,
                    r.cost_budget,
                    r.cost_used,
                    r.time_budget_seconds,
                    r.last_checkpoint,
                    r.started_at,
                    r.completed_at,
                    r.updated_at
                FROM novel_agent_run r
                JOIN novel_project p ON p.id = r.project_id
                WHERE r.state IN (
                    'READY_TO_WRITE',
                    'GENERATING_CHAPTER',
                    'VALIDATING_OUTPUT',
                    'REVIEWING_QUALITY',
                    'CHECKING_CONTINUITY',
                    'REVISING_CHAPTER',
                    'COMMITTING_CHAPTER'
                )
                ORDER BY r.created_at
                """,
                this::mapRun);
    }

    public void prepareInterruptedRunForRecovery(AgentRunSnapshot run) {
        if (run.state() == AgentRunState.READY_TO_WRITE) {
            return;
        }
        int updated = jdbcTemplate.update(
                """
                UPDATE novel_agent_run
                SET state = ?, current_step = NULL,
                    pause_reason = 'RECOVERED_AFTER_RESTART',
                    version = version + 1
                WHERE id = ? AND state = ?
                """,
                AgentRunState.READY_TO_WRITE.name(),
                run.runId(),
                run.state().name());
        requireSingleUpdate(updated, "恢复运行时状态已发生变化");
    }

    public void transition(
            String runId,
            AgentRunState expected,
            AgentRunState next,
            AgentStepType currentStep) {
        int updated = jdbcTemplate.update(
                """
                UPDATE novel_agent_run
                SET state = ?, current_step = ?, version = version + 1
                WHERE id = ? AND state = ?
                """,
                next.name(),
                currentStep == null ? null : currentStep.name(),
                runId,
                expected.name());
        requireSingleUpdate(updated, "运行状态已变化，不能执行 " + expected + " -> " + next);
    }

    public String recordStepSuccess(
            String runId,
            AgentStepType stepType,
            String subjectKey,
            int revisionNo,
            Object input,
            Object output) {
        String stepId = uuid();
        String idempotencyKey = "run:%s:step:%s:subject:%s:revision:%d".formatted(
                runId,
                stepType.name(),
                subjectKey == null ? "run" : subjectKey,
                revisionNo);
        jdbcTemplate.update(
                """
                INSERT INTO novel_agent_step (
                    id, run_id, step_type, status, subject_key, revision_no,
                    attempt_no, max_attempts, idempotency_key, input_json,
                    output_json, started_at, completed_at
                ) VALUES (?, ?, ?, 'SUCCEEDED', ?, ?, 1, 2, ?, CAST(? AS JSON),
                          CAST(? AS JSON), CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """,
                stepId,
                runId,
                stepType.name(),
                subjectKey,
                revisionNo,
                idempotencyKey,
                toJson(input),
                toJson(output));
        return stepId;
    }

    @Transactional
    public void saveProposalsAndWait(
            String runId,
            String projectId,
            List<CreativeProposalDraft> proposals) {
        for (CreativeProposalDraft proposal : proposals) {
            jdbcTemplate.update(
                    """
                    INSERT INTO novel_creative_proposal (
                        id, run_id, project_id, proposal_no, title, premise,
                        target_audience, market_rationale, differentiator,
                        risk_summary, proposal_json
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON))
                    """,
                    uuid(),
                    runId,
                    projectId,
                    proposal.proposalNo(),
                    proposal.title(),
                    proposal.premise(),
                    proposal.targetAudience(),
                    proposal.marketRationale(),
                    proposal.differentiator(),
                    proposal.riskSummary(),
                    toJson(proposal.details()));
        }
        int updated = jdbcTemplate.update(
                """
                UPDATE novel_agent_run
                SET state = ?, current_step = NULL, version = version + 1
                WHERE id = ? AND state = ?
                """,
                AgentRunState.WAITING_CREATIVE_SELECTION.name(),
                runId,
                AgentRunState.GENERATING_PROPOSALS.name());
        requireSingleUpdate(updated, "保存创意时运行状态已变化");
    }

    public List<CreativeProposalRecord> findProposals(String runId) {
        return jdbcTemplate.query(
                """
                SELECT id, run_id, project_id, proposal_no, title, premise,
                       target_audience, market_rationale, differentiator,
                       risk_summary, selected
                FROM novel_creative_proposal
                WHERE run_id = ?
                ORDER BY proposal_no
                """,
                (resultSet, rowNum) -> new CreativeProposalRecord(
                        resultSet.getString("id"),
                        resultSet.getString("run_id"),
                        resultSet.getString("project_id"),
                        resultSet.getInt("proposal_no"),
                        resultSet.getString("title"),
                        resultSet.getString("premise"),
                        resultSet.getString("target_audience"),
                        resultSet.getString("market_rationale"),
                        resultSet.getString("differentiator"),
                        resultSet.getString("risk_summary"),
                        resultSet.getBoolean("selected")),
                runId);
    }

    public Optional<CreativeProposalRecord> findProposal(String runId, String proposalId) {
        return findProposals(runId).stream()
                .filter(proposal -> proposal.id().equals(proposalId))
                .findFirst();
    }

    @Transactional
    public void selectProposal(
            AgentRunSnapshot run,
            CreativeProposalRecord proposal) {
        jdbcTemplate.update(
                "UPDATE novel_creative_proposal SET selected = FALSE, selected_at = NULL WHERE run_id = ?",
                run.runId());
        int selected = jdbcTemplate.update(
                """
                UPDATE novel_creative_proposal
                SET selected = TRUE, selected_at = CURRENT_TIMESTAMP(6)
                WHERE id = ? AND run_id = ?
                """,
                proposal.id(),
                run.runId());
        requireSingleUpdate(selected, "创意不存在或不属于当前运行");

        jdbcTemplate.update(
                """
                UPDATE novel_project
                SET selected_proposal_id = ?, status = 'INITIALIZING', version = version + 1
                WHERE id = ?
                """,
                proposal.id(),
                run.projectId());
        int transitioned = jdbcTemplate.update(
                """
                UPDATE novel_agent_run
                SET state = ?, current_step = ?, last_checkpoint = 'CREATIVE_SELECTED',
                    version = version + 1
                WHERE id = ? AND state = ?
                """,
                AgentRunState.INITIALIZING_NOVEL.name(),
                AgentStepType.INITIALIZE_FOUNDATION.name(),
                run.runId(),
                AgentRunState.WAITING_CREATIVE_SELECTION.name());
        requireSingleUpdate(transitioned, "当前运行不在等待创意选择状态");

        insertCheckpoint(
                run.runId(),
                run.projectId(),
                "CREATIVE_SELECTED",
                0,
                AgentRunState.INITIALIZING_NOVEL,
                Map.of("proposalId", proposal.id(), "proposalNo", proposal.proposalNo()));
    }

    @Transactional
    public void commitFoundation(
            AgentRunSnapshot run,
            FoundationDraft foundation,
            List<ChapterPlanDraft> plans) {
        String foundationId = uuid();
        jdbcTemplate.update(
                """
                INSERT INTO novel_foundation (
                    id, project_id, run_id, version_no, status, world_json,
                    characters_json, relationships_json, main_plot_json,
                    stages_json, foreshadowing_json, story_bible_json, confirmed_at
                ) VALUES (?, ?, ?, 1, 'CONFIRMED', CAST(? AS JSON), CAST(? AS JSON),
                          CAST(? AS JSON), CAST(? AS JSON), CAST(? AS JSON),
                          CAST(? AS JSON), CAST(? AS JSON), CURRENT_TIMESTAMP(6))
                """,
                foundationId,
                run.projectId(),
                run.runId(),
                toJson(foundation.world()),
                toJson(foundation.characters()),
                toJson(foundation.relationships()),
                toJson(foundation.mainPlot()),
                toJson(foundation.stages()),
                toJson(foundation.foreshadowing()),
                toJson(foundation.storyBibleFacts()));

        for (ChapterPlanDraft plan : plans) {
            jdbcTemplate.update(
                    """
                    INSERT INTO novel_chapter_plan (
                        id, project_id, foundation_id, chapter_no, title, objective,
                        conflict_summary, key_events_json, character_changes_json,
                        foreshadowing_actions_json, climax, ending_hook,
                        forbidden_events_json, status
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), CAST(? AS JSON),
                              CAST(? AS JSON), ?, ?, CAST(? AS JSON), 'PLANNED')
                    """,
                    uuid(),
                    run.projectId(),
                    foundationId,
                    plan.chapterNo(),
                    plan.title(),
                    plan.objective(),
                    plan.conflictSummary(),
                    toJson(plan.keyEvents()),
                    toJson(plan.characterChanges()),
                    toJson(plan.foreshadowingActions()),
                    plan.climax(),
                    plan.endingHook(),
                    toJson(plan.forbiddenEvents()));
        }

        for (Map<String, Object> fact : foundation.storyBibleFacts()) {
            jdbcTemplate.update(
                    """
                    INSERT INTO novel_story_fact (
                        id, project_id, source_type, source_id, fact_type,
                        subject_key, predicate_key, value_json, status,
                        effective_chapter_no, version_no
                    ) VALUES (?, ?, 'FOUNDATION', ?, ?, ?, ?, CAST(? AS JSON),
                              'ACTIVE', 0, 1)
                    """,
                    uuid(),
                    run.projectId(),
                    foundationId,
                    String.valueOf(fact.get("factType")),
                    String.valueOf(fact.get("subject")),
                    String.valueOf(fact.get("predicate")),
                    toJson(fact.get("value")));
        }

        jdbcTemplate.update(
                """
                UPDATE novel_project
                SET title = ?, status = 'WRITING', foundation_version = 1,
                    version = version + 1
                WHERE id = ?
                """,
                foundation.novelTitle(),
                run.projectId());
        int transitioned = jdbcTemplate.update(
                """
                UPDATE novel_agent_run
                SET state = ?, current_step = NULL, last_checkpoint = 'FOUNDATION_CONFIRMED',
                    version = version + 1
                WHERE id = ? AND state = ?
                """,
                AgentRunState.READY_TO_WRITE.name(),
                run.runId(),
                AgentRunState.PLANNING_CHAPTERS.name());
        requireSingleUpdate(transitioned, "初始化提交时运行状态已变化");

        insertCheckpoint(
                run.runId(),
                run.projectId(),
                "FOUNDATION_CONFIRMED",
                0,
                AgentRunState.READY_TO_WRITE,
                Map.of("foundationId", foundationId, "foundationVersion", 1));
    }

    public Optional<ChapterPlanRecord> findChapterPlan(String projectId, int chapterNo) {
        List<ChapterPlanRecord> result = jdbcTemplate.query(
                """
                SELECT id, project_id, chapter_no, title, objective,
                       conflict_summary, climax, ending_hook
                FROM novel_chapter_plan
                WHERE project_id = ? AND chapter_no = ?
                """,
                (resultSet, rowNum) -> new ChapterPlanRecord(
                        resultSet.getString("id"),
                        resultSet.getString("project_id"),
                        resultSet.getInt("chapter_no"),
                        resultSet.getString("title"),
                        resultSet.getString("objective"),
                        resultSet.getString("conflict_summary"),
                        resultSet.getString("climax"),
                        resultSet.getString("ending_hook")),
                projectId,
                chapterNo);
        return result.stream().findFirst();
    }

    public int nextChapterRevision(String runId, int chapterNo) {
        Integer nextRevision = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(MAX(revision_no), -1) + 1
                FROM novel_agent_step
                WHERE run_id = ? AND subject_key = ?
                """,
                Integer.class,
                runId,
                "chapter:" + chapterNo);
        return nextRevision == null ? 0 : nextRevision;
    }

    @Transactional
    public void commitChapter(
            AgentRunSnapshot run,
            ChapterPlanRecord plan,
            ChapterDraft draft,
            ReviewReport qualityReport,
            ReviewReport continuityReport) {
        String chapterId = uuid();
        int wordCount = countNonWhitespaceCodePoints(draft.content());
        jdbcTemplate.update(
                """
                INSERT INTO novel_chapter (
                    id, project_id, run_id, chapter_plan_id, chapter_no, version_no,
                    title, content, summary, metadata_json, quality_report_json,
                    continuity_report_json, word_count, status
                ) VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?, CAST(? AS JSON),
                          CAST(? AS JSON), CAST(? AS JSON), ?, 'CONFIRMED')
                """,
                chapterId,
                run.projectId(),
                run.runId(),
                plan.id(),
                draft.chapterNo(),
                draft.title(),
                draft.content(),
                draft.summary(),
                toJson(draft.metadata()),
                toJson(qualityReport),
                toJson(continuityReport),
                wordCount);

        jdbcTemplate.update(
                """
                INSERT INTO novel_story_fact (
                    id, project_id, source_type, source_id, fact_type, subject_key,
                    predicate_key, value_json, status, effective_chapter_no, version_no
                ) VALUES (?, ?, 'CHAPTER', ?, 'CHAPTER_SUMMARY', ?, 'summary',
                          CAST(? AS JSON), 'ACTIVE', ?, 1)
                """,
                uuid(),
                run.projectId(),
                chapterId,
                "chapter:" + draft.chapterNo(),
                toJson(draft.summary()),
                draft.chapterNo());
        jdbcTemplate.update(
                "UPDATE novel_chapter_plan SET status = 'COMPLETED' WHERE id = ?",
                plan.id());

        boolean completed = draft.chapterNo() >= run.targetChapterNo();
        String projectStatus = completed ? "PROTOTYPE_COMPLETED" : "WRITING";
        AgentRunState nextState = completed
                ? AgentRunState.COMPLETED
                : AgentRunState.READY_TO_WRITE;
        jdbcTemplate.update(
                """
                UPDATE novel_project
                SET current_chapter_no = ?, status = ?, version = version + 1
                WHERE id = ?
                """,
                draft.chapterNo(),
                projectStatus,
                run.projectId());
        int transitioned = jdbcTemplate.update(
                """
                UPDATE novel_agent_run
                SET current_chapter_no = ?, state = ?, current_step = NULL,
                    current_revision_no = 0, last_checkpoint = ?,
                    completed_at = CASE WHEN ? THEN CURRENT_TIMESTAMP(6) ELSE NULL END,
                    version = version + 1
                WHERE id = ? AND state = ?
                """,
                draft.chapterNo(),
                nextState.name(),
                "CHAPTER_" + draft.chapterNo() + "_CONFIRMED",
                completed,
                run.runId(),
                AgentRunState.COMMITTING_CHAPTER.name());
        requireSingleUpdate(transitioned, "章节提交时运行状态已变化");

        insertCheckpoint(
                run.runId(),
                run.projectId(),
                "CHAPTER_CONFIRMED",
                draft.chapterNo(),
                nextState,
                Map.of("chapterId", chapterId, "chapterNo", draft.chapterNo()));
    }

    @Transactional
    public String createDecisionAndPause(
            AgentRunSnapshot run,
            String triggeringStepId,
            AgentRunState expectedState,
            String decisionType,
            String title,
            String context,
            Object options,
            Object recommendation,
            Object impact) {
        String decisionId = uuid();
        jdbcTemplate.update(
                """
                INSERT INTO novel_decision_request (
                    id, run_id, project_id, triggering_step_id, decision_type,
                    priority, status, title, context_text, options_json,
                    recommendation_json, impact_json, resume_state, resume_step
                ) VALUES (?, ?, ?, ?, ?, 'HIGH', 'OPEN', ?, ?, CAST(? AS JSON),
                          CAST(? AS JSON), CAST(? AS JSON), ?, ?)
                """,
                decisionId,
                run.runId(),
                run.projectId(),
                triggeringStepId,
                decisionType,
                title,
                context,
                toJson(options),
                toJson(recommendation),
                toJson(impact),
                AgentRunState.READY_TO_WRITE.name(),
                AgentStepType.GENERATE_CHAPTER.name());
        int transitioned = jdbcTemplate.update(
                """
                UPDATE novel_agent_run
                SET state = ?, current_step = NULL, pause_reason = ?,
                    paused_at = CURRENT_TIMESTAMP(6), version = version + 1
                WHERE id = ? AND state = ?
                """,
                AgentRunState.WAITING_USER_DECISION.name(),
                decisionType,
                run.runId(),
                expectedState.name());
        requireSingleUpdate(transitioned, "创建决策时运行状态已变化");
        return decisionId;
    }

    public List<DecisionRecord> findOpenDecisions(String runId) {
        return jdbcTemplate.query(
                """
                SELECT id, decision_type, priority, status, title, context_text,
                       CAST(options_json AS CHAR) AS options_json,
                       CAST(recommendation_json AS CHAR) AS recommendation_json,
                       created_at
                FROM novel_decision_request
                WHERE run_id = ? AND status = 'OPEN'
                ORDER BY created_at
                """,
                (resultSet, rowNum) -> new DecisionRecord(
                        resultSet.getString("id"),
                        resultSet.getString("decision_type"),
                        resultSet.getString("priority"),
                        resultSet.getString("status"),
                        resultSet.getString("title"),
                        resultSet.getString("context_text"),
                        resultSet.getString("options_json"),
                        resultSet.getString("recommendation_json"),
                        resultSet.getObject("created_at", LocalDateTime.class)),
                runId);
    }

    @Transactional
    public void resolveDecision(
            AgentRunSnapshot run,
            String decisionId,
            Object answer) {
        int resolved = jdbcTemplate.update(
                """
                UPDATE novel_decision_request
                SET status = 'RESOLVED', answer_json = CAST(? AS JSON),
                    resolved_at = CURRENT_TIMESTAMP(6)
                WHERE id = ? AND run_id = ? AND status = 'OPEN'
                """,
                toJson(answer),
                decisionId,
                run.runId());
        requireSingleUpdate(resolved, "待决策事项不存在或已经处理");

        int transitioned = jdbcTemplate.update(
                """
                UPDATE novel_agent_run
                SET state = ?, current_step = NULL, pause_reason = NULL,
                    paused_at = NULL, version = version + 1
                WHERE id = ? AND state = ?
                """,
                AgentRunState.READY_TO_WRITE.name(),
                run.runId(),
                AgentRunState.WAITING_USER_DECISION.name());
        requireSingleUpdate(transitioned, "当前运行不在等待用户决策状态");

        insertCheckpoint(
                run.runId(),
                run.projectId(),
                "DECISION_RESOLVED",
                run.currentChapterNo(),
                AgentRunState.READY_TO_WRITE,
                Map.of("decisionId", decisionId, "answer", answer));
    }

    public boolean hasResolvedDecision(String runId, String decisionType) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM novel_decision_request
                WHERE run_id = ? AND decision_type = ? AND status = 'RESOLVED'
                """,
                Integer.class,
                runId,
                decisionType);
        return count != null && count > 0;
    }

    public void resumeWithBudgets(
            AgentRunSnapshot run,
            Long tokenBudget,
            BigDecimal costBudget,
            Integer timeBudgetSeconds) {
        int updated = jdbcTemplate.update(
                """
                UPDATE novel_agent_run
                SET state = ?, current_step = NULL, pause_reason = NULL,
                    paused_at = NULL,
                    token_budget = COALESCE(?, token_budget),
                    cost_budget = COALESCE(?, cost_budget),
                    time_budget_seconds = COALESCE(?, time_budget_seconds),
                    version = version + 1
                WHERE id = ? AND state IN (?, ?)
                """,
                AgentRunState.READY_TO_WRITE.name(),
                tokenBudget,
                costBudget,
                timeBudgetSeconds,
                run.runId(),
                AgentRunState.PAUSED.name(),
                AgentRunState.PAUSED_BY_BUDGET.name());
        requireSingleUpdate(updated, "当前运行不是可恢复的暂停状态");
    }

    public Optional<FoundationDraft> findLatestFoundation(String projectId) {
        List<FoundationDraft> foundations = jdbcTemplate.query(
                """
                SELECT p.title, f.world_json, f.characters_json, f.relationships_json,
                       f.main_plot_json, f.stages_json, f.foreshadowing_json,
                       f.story_bible_json
                FROM novel_foundation f
                JOIN novel_project p ON p.id = f.project_id
                WHERE f.project_id = ? AND f.status = 'CONFIRMED'
                ORDER BY f.version_no DESC
                LIMIT 1
                """,
                (resultSet, rowNum) -> new FoundationDraft(
                        resultSet.getString("title"),
                        readMap(resultSet.getString("world_json")),
                        readListOfMaps(resultSet.getString("characters_json")),
                        readListOfMaps(resultSet.getString("relationships_json")),
                        readMap(resultSet.getString("main_plot_json")),
                        readListOfMaps(resultSet.getString("stages_json")),
                        readListOfMaps(resultSet.getString("foreshadowing_json")),
                        readListOfMaps(resultSet.getString("story_bible_json"))),
                projectId);
        return foundations.stream().findFirst();
    }

    public List<ChapterRecord> findChapters(String projectId) {
        return jdbcTemplate.query(
                """
                SELECT id, chapter_no, title, content, summary, word_count, version_no
                FROM novel_chapter
                WHERE project_id = ? AND status = 'CONFIRMED'
                ORDER BY chapter_no, version_no
                """,
                (resultSet, rowNum) -> new ChapterRecord(
                        resultSet.getString("id"),
                        resultSet.getInt("chapter_no"),
                        resultSet.getString("title"),
                        resultSet.getString("content"),
                        resultSet.getString("summary"),
                        resultSet.getInt("word_count"),
                        resultSet.getInt("version_no")),
                projectId);
    }

    @Transactional
    public void recordModelCallAndUsage(
            String runId,
            String stepId,
            String provider,
            String model,
            AgentStepType taskType,
            long inputTokens,
            long outputTokens,
            BigDecimal cost,
            long durationMs) {
        jdbcTemplate.update(
                """
                INSERT INTO model_call_record (
                    id, run_id, step_id, provider, model_name, task_type,
                    prompt_template_version, input_tokens, output_tokens,
                    cost_amount, duration_ms, status, completed_at
                ) VALUES (?, ?, ?, ?, ?, ?, 'fake-v1', ?, ?, ?, ?, 'SUCCEEDED',
                          CURRENT_TIMESTAMP(6))
                """,
                uuid(),
                runId,
                stepId,
                provider,
                model,
                taskType.name(),
                inputTokens,
                outputTokens,
                cost,
                durationMs);
        jdbcTemplate.update(
                """
                UPDATE novel_agent_run
                SET token_used = token_used + ?, cost_used = cost_used + ?
                WHERE id = ?
                """,
                inputTokens + outputTokens,
                cost,
                runId);
    }

    private void insertCheckpoint(
            String runId,
            String projectId,
            String checkpointType,
            int chapterNo,
            AgentRunState state,
            Object snapshot) {
        jdbcTemplate.update(
                """
                INSERT INTO novel_checkpoint (
                    id, run_id, project_id, checkpoint_type, chapter_no,
                    state, snapshot_json
                ) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS JSON))
                """,
                uuid(),
                runId,
                projectId,
                checkpointType,
                chapterNo,
                state.name(),
                toJson(snapshot));
    }

    private AgentRunSnapshot mapRun(ResultSet resultSet, int rowNum) throws SQLException {
        return new AgentRunSnapshot(
                resultSet.getString("run_id"),
                resultSet.getString("project_id"),
                resultSet.getString("original_idea"),
                resultSet.getString("genre"),
                resultSet.getString("title"),
                resultSet.getString("project_status"),
                AgentRunState.valueOf(resultSet.getString("state")),
                resultSet.getString("current_step"),
                resultSet.getInt("current_chapter_no"),
                resultSet.getInt("target_chapter_no"),
                resultSet.getLong("token_budget"),
                resultSet.getLong("token_used"),
                resultSet.getBigDecimal("cost_budget"),
                resultSet.getBigDecimal("cost_used"),
                resultSet.getInt("time_budget_seconds"),
                resultSet.getString("last_checkpoint"),
                resultSet.getObject("started_at", LocalDateTime.class),
                resultSet.getObject("completed_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化 NovelAgent 数据", exception);
        }
    }

    private Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法读取小说基础JSON对象", exception);
        }
    }

    private List<Map<String, Object>> readListOfMaps(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法读取小说基础JSON列表", exception);
        }
    }

    private static void requireSingleUpdate(int count, String message) {
        if (count != 1) {
            throw new IllegalStateException(message);
        }
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }

    private static int countNonWhitespaceCodePoints(String content) {
        return (int) content.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .count();
    }
}

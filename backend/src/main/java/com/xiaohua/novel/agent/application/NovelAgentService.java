package com.xiaohua.novel.agent.application;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.xiaohua.novel.agent.domain.AgentRunSnapshot;
import com.xiaohua.novel.agent.domain.AgentRunState;
import com.xiaohua.novel.agent.domain.AgentStateMachine;
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
import com.xiaohua.novel.agent.domain.MarketResearchResult;
import com.xiaohua.novel.agent.domain.ReviewIssue;
import com.xiaohua.novel.agent.domain.ReviewReport;
import com.xiaohua.novel.agent.infrastructure.NovelAgentRepository;
import com.xiaohua.novel.agent.provider.NovelModelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NovelAgentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NovelAgentService.class);
    private static final int DEFAULT_TARGET_CHAPTERS = 3;
    private static final long DEFAULT_TOKEN_BUDGET = 100_000L;
    private static final int DEFAULT_TIME_BUDGET_SECONDS = 3600;
    private static final int MAX_AUTOMATIC_REVISIONS = 2;

    private final NovelAgentRepository repository;
    private final NovelModelProvider modelProvider;
    private final AgentStateMachine stateMachine;

    public NovelAgentService(
            NovelAgentRepository repository,
            NovelModelProvider modelProvider,
            AgentStateMachine stateMachine) {
        this.repository = repository;
        this.modelProvider = modelProvider;
        this.stateMachine = stateMachine;
    }

    public AgentRunSnapshot createRun(CreateNovelRunCommand command) {
        int targetChapters = command.targetChapters() == null
                ? DEFAULT_TARGET_CHAPTERS
                : command.targetChapters();
        if (targetChapters < 1 || targetChapters > 3) {
            throw new IllegalArgumentException("第一阶段目标章节数必须在1到3之间");
        }

        long tokenBudget = command.tokenBudget() == null
                ? DEFAULT_TOKEN_BUDGET
                : command.tokenBudget();
        BigDecimal costBudget = command.costBudget() == null
                ? BigDecimal.ZERO
                : command.costBudget();
        int timeBudgetSeconds = command.timeBudgetSeconds() == null
                ? DEFAULT_TIME_BUDGET_SECONDS
                : command.timeBudgetSeconds();

        CreatedRun createdRun = repository.createRun(
                command.idea().trim(),
                normalize(command.genre()),
                targetChapters,
                tokenBudget,
                costBudget,
                timeBudgetSeconds);
        generateCreativeProposals(createdRun.runId());
        return getRun(createdRun.runId());
    }

    public AgentRunSnapshot getRun(String runId) {
        return repository.findRun(runId)
                .orElseThrow(() -> new NovelAgentNotFoundException("NovelAgent 运行不存在"));
    }

    public List<CreativeProposalRecord> getProposals(String runId) {
        getRun(runId);
        return repository.findProposals(runId);
    }

    public List<ChapterRecord> getChapters(String runId) {
        AgentRunSnapshot run = getRun(runId);
        return repository.findChapters(run.projectId());
    }

    public List<DecisionRecord> getOpenDecisions(String runId) {
        getRun(runId);
        return repository.findOpenDecisions(runId);
    }

    public AgentRunSnapshot selectCreative(
            String runId,
            String proposalId,
            String adjustments) {
        AgentRunSnapshot run = getRun(runId);
        if (run.state() != AgentRunState.WAITING_CREATIVE_SELECTION) {
            throw new NovelAgentConflictException("当前运行不在等待创意选择状态");
        }
        CreativeProposalRecord proposal = repository.findProposal(runId, proposalId)
                .orElseThrow(() -> new NovelAgentNotFoundException("创意方案不存在"));

        stateMachine.requireTransition(
                AgentRunState.WAITING_CREATIVE_SELECTION,
                AgentRunState.INITIALIZING_NOVEL);
        repository.selectProposal(run, proposal);

        FoundationDraft foundation = initializeNovel(
                getRun(runId),
                proposal,
                normalize(adjustments));
        writeUntilTarget(runId, foundation);
        return getRun(runId);
    }

    public AgentRunSnapshot resolveDecision(
            String runId,
            String decisionId,
            String optionCode,
            String comment) {
        AgentRunSnapshot run = getRun(runId);
        if (run.state() != AgentRunState.WAITING_USER_DECISION) {
            throw new NovelAgentConflictException("当前运行不在等待用户决策状态");
        }
        stateMachine.requireTransition(
                AgentRunState.WAITING_USER_DECISION,
                AgentRunState.READY_TO_WRITE);
        repository.resolveDecision(
                run,
                decisionId,
                java.util.Map.of(
                        "optionCode", optionCode,
                        "comment", comment == null ? "" : comment));
        FoundationDraft foundation = repository.findLatestFoundation(run.projectId())
                .orElseThrow(() -> new NovelAgentConflictException("缺少已确认的小说基础版本"));
        writeUntilTarget(runId, foundation);
        return getRun(runId);
    }

    public AgentRunSnapshot resumeRun(
            String runId,
            Long tokenBudget,
            BigDecimal costBudget,
            Integer timeBudgetSeconds) {
        AgentRunSnapshot run = getRun(runId);
        if (run.state() != AgentRunState.PAUSED
                && run.state() != AgentRunState.PAUSED_BY_BUDGET) {
            throw new NovelAgentConflictException("当前运行不是可恢复的暂停状态");
        }
        stateMachine.requireTransition(run.state(), AgentRunState.READY_TO_WRITE);
        repository.resumeWithBudgets(
                run,
                tokenBudget,
                costBudget,
                timeBudgetSeconds);
        FoundationDraft foundation = repository.findLatestFoundation(run.projectId())
                .orElseThrow(() -> new NovelAgentConflictException("缺少已确认的小说基础版本"));
        writeUntilTarget(runId, foundation);
        return getRun(runId);
    }

    public void recoverInterruptedChapterRuns() {
        for (AgentRunSnapshot interruptedRun : repository.findInterruptedChapterRuns()) {
            try {
                repository.prepareInterruptedRunForRecovery(interruptedRun);
                FoundationDraft foundation = repository
                        .findLatestFoundation(interruptedRun.projectId())
                        .orElseThrow(() -> new NovelAgentConflictException(
                                "缺少已确认的小说基础版本"));
                writeUntilTarget(interruptedRun.runId(), foundation);
            } catch (RuntimeException exception) {
                LOGGER.error(
                        "恢复 NovelAgent 运行 {} 失败",
                        interruptedRun.runId(),
                        exception);
                AgentRunSnapshot current = getRun(interruptedRun.runId());
                if (current.state() != AgentRunState.FAILED
                        && stateMachine.canTransition(
                                current.state(),
                                AgentRunState.FAILED)) {
                    repository.transition(
                            current.runId(),
                            current.state(),
                            AgentRunState.FAILED,
                            null);
                }
            }
        }
    }

    private void generateCreativeProposals(String runId) {
        AgentRunSnapshot run = getRun(runId);
        stateMachine.requireTransition(run.state(), AgentRunState.RESEARCHING);
        repository.transition(
                runId,
                run.state(),
                AgentRunState.RESEARCHING,
                AgentStepType.RESEARCH_MARKET);

        long started = System.nanoTime();
        MarketResearchResult research = modelProvider.research(
                run.originalIdea(),
                run.genre());
        recordModelStep(
                runId,
                AgentStepType.RESEARCH_MARKET,
                "run",
                0,
                run.originalIdea(),
                research,
                started);

        stateMachine.requireTransition(
                AgentRunState.RESEARCHING,
                AgentRunState.GENERATING_PROPOSALS);
        repository.transition(
                runId,
                AgentRunState.RESEARCHING,
                AgentRunState.GENERATING_PROPOSALS,
                AgentStepType.GENERATE_PROPOSALS);

        started = System.nanoTime();
        List<CreativeProposalDraft> proposals = modelProvider.generateProposals(
                run.originalIdea(),
                run.genre(),
                research);
        validateProposals(proposals);
        recordModelStep(
                runId,
                AgentStepType.GENERATE_PROPOSALS,
                "run",
                0,
                research,
                proposals,
                started);

        stateMachine.requireTransition(
                AgentRunState.GENERATING_PROPOSALS,
                AgentRunState.WAITING_CREATIVE_SELECTION);
        repository.saveProposalsAndWait(runId, run.projectId(), proposals);
    }

    private FoundationDraft initializeNovel(
            AgentRunSnapshot run,
            CreativeProposalRecord proposal,
            String adjustments) {
        long started = System.nanoTime();
        FoundationDraft foundation = modelProvider.initializeNovel(
                run.originalIdea(),
                run.genre(),
                proposal,
                adjustments);
        validateFoundation(foundation);
        recordModelStep(
                run.runId(),
                AgentStepType.INITIALIZE_FOUNDATION,
                "foundation",
                0,
                proposal,
                foundation,
                started);

        stateMachine.requireTransition(
                AgentRunState.INITIALIZING_NOVEL,
                AgentRunState.PLANNING_CHAPTERS);
        repository.transition(
                run.runId(),
                AgentRunState.INITIALIZING_NOVEL,
                AgentRunState.PLANNING_CHAPTERS,
                AgentStepType.PLAN_CHAPTERS);

        started = System.nanoTime();
        List<ChapterPlanDraft> plans = modelProvider.planChapters(foundation, 10);
        validatePlans(plans);
        recordModelStep(
                run.runId(),
                AgentStepType.PLAN_CHAPTERS,
                "stage:1",
                0,
                foundation.mainPlot(),
                plans,
                started);

        stateMachine.requireTransition(
                AgentRunState.PLANNING_CHAPTERS,
                AgentRunState.READY_TO_WRITE);
        repository.commitFoundation(getRun(run.runId()), foundation, plans);
        return foundation;
    }

    private void writeUntilTarget(String runId, FoundationDraft foundation) {
        while (true) {
            AgentRunSnapshot run = getRun(runId);
            if (run.state() == AgentRunState.COMPLETED
                    || run.state() == AgentRunState.WAITING_USER_DECISION
                    || run.state() == AgentRunState.PAUSED_BY_BUDGET
                    || run.state() == AgentRunState.FAILED) {
                return;
            }
            if (run.state() != AgentRunState.READY_TO_WRITE) {
                throw new NovelAgentConflictException("运行无法从当前状态继续写作: " + run.state());
            }
            if (budgetExhausted(run)) {
                stateMachine.requireTransition(run.state(), AgentRunState.PAUSED_BY_BUDGET);
                repository.transition(
                        runId,
                        run.state(),
                        AgentRunState.PAUSED_BY_BUDGET,
                        null);
                return;
            }
            writeNextChapter(run, foundation);
        }
    }

    private void writeNextChapter(
            AgentRunSnapshot run,
            FoundationDraft foundation) {
        int chapterNo = run.currentChapterNo() + 1;
        ChapterPlanRecord plan = repository.findChapterPlan(run.projectId(), chapterNo)
                .orElseThrow(() -> new NovelAgentConflictException(
                        "缺少第" + chapterNo + "章章节卡"));

        stateMachine.requireTransition(run.state(), AgentRunState.GENERATING_CHAPTER);
        repository.transition(
                run.runId(),
                run.state(),
                AgentRunState.GENERATING_CHAPTER,
                AgentStepType.GENERATE_CHAPTER);

        long started = System.nanoTime();
        String writingIdea = repository.hasResolvedDecision(
                run.runId(),
                "MAJOR_CHARACTER_FATE")
                ? run.originalIdea().replace("重要人物死亡", "重要人物存活")
                : run.originalIdea();
        int generationRevision = repository.nextChapterRevision(
                run.runId(),
                chapterNo);
        ChapterDraft generatedDraft = modelProvider.writeChapter(
                writingIdea,
                foundation,
                plan);
        ChapterDraft draft = new ChapterDraft(
                generatedDraft.chapterNo(),
                generatedDraft.title(),
                generatedDraft.content(),
                generatedDraft.summary(),
                generatedDraft.metadata(),
                generationRevision);
        recordModelStep(
                run.runId(),
                AgentStepType.GENERATE_CHAPTER,
                "chapter:" + chapterNo,
                draft.revisionNo(),
                plan,
                draft,
                started);

        stateMachine.requireTransition(
                AgentRunState.GENERATING_CHAPTER,
                AgentRunState.VALIDATING_OUTPUT);
        repository.transition(
                run.runId(),
                AgentRunState.GENERATING_CHAPTER,
                AgentRunState.VALIDATING_OUTPUT,
                AgentStepType.VALIDATE_OUTPUT);
        validateChapter(draft, chapterNo);
        repository.recordStepSuccess(
                run.runId(),
                AgentStepType.VALIDATE_OUTPUT,
                "chapter:" + chapterNo,
                draft.revisionNo(),
                draft.metadata(),
                java.util.Map.of("valid", true));

        stateMachine.requireTransition(
                AgentRunState.VALIDATING_OUTPUT,
                AgentRunState.REVIEWING_QUALITY);
        repository.transition(
                run.runId(),
                AgentRunState.VALIDATING_OUTPUT,
                AgentRunState.REVIEWING_QUALITY,
                AgentStepType.REVIEW_QUALITY);

        ReviewReport qualityReport = reviewQuality(run, draft);
        int revisions = 0;
        while (!qualityReport.passed() && !qualityReport.requiresUserDecision()) {
            if (revisions >= MAX_AUTOMATIC_REVISIONS) {
                pauseForDecision(
                        getRun(run.runId()),
                        null,
                        AgentRunState.REVIEWING_QUALITY,
                        "REVISION_LIMIT_REACHED",
                        "章节自动修订达到上限",
                        "第" + chapterNo + "章连续修订后仍未通过质量检查。");
                return;
            }
            stateMachine.requireTransition(
                    AgentRunState.REVIEWING_QUALITY,
                    AgentRunState.REVISING_CHAPTER);
            repository.transition(
                    run.runId(),
                    AgentRunState.REVIEWING_QUALITY,
                    AgentRunState.REVISING_CHAPTER,
                    AgentStepType.REVISE_CHAPTER);

            started = System.nanoTime();
            draft = modelProvider.revise(draft, qualityReport);
            revisions++;
            recordModelStep(
                    run.runId(),
                    AgentStepType.REVISE_CHAPTER,
                    "chapter:" + chapterNo,
                    draft.revisionNo(),
                    qualityReport,
                    draft,
                    started);

            stateMachine.requireTransition(
                    AgentRunState.REVISING_CHAPTER,
                    AgentRunState.VALIDATING_OUTPUT);
            repository.transition(
                    run.runId(),
                    AgentRunState.REVISING_CHAPTER,
                    AgentRunState.VALIDATING_OUTPUT,
                    AgentStepType.VALIDATE_OUTPUT);
            validateChapter(draft, chapterNo);
            repository.recordStepSuccess(
                    run.runId(),
                    AgentStepType.VALIDATE_OUTPUT,
                    "chapter:" + chapterNo,
                    draft.revisionNo(),
                    draft.metadata(),
                    java.util.Map.of("valid", true));

            stateMachine.requireTransition(
                    AgentRunState.VALIDATING_OUTPUT,
                    AgentRunState.REVIEWING_QUALITY);
            repository.transition(
                    run.runId(),
                    AgentRunState.VALIDATING_OUTPUT,
                    AgentRunState.REVIEWING_QUALITY,
                    AgentStepType.REVIEW_QUALITY);
            qualityReport = reviewQuality(run, draft);
        }

        if (qualityReport.requiresUserDecision()) {
            pauseForDecision(
                    getRun(run.runId()),
                    null,
                    AgentRunState.REVIEWING_QUALITY,
                    firstIssueCode(qualityReport),
                    "章节质量问题需要用户决策",
                    firstIssueMessage(qualityReport));
            return;
        }

        stateMachine.requireTransition(
                AgentRunState.REVIEWING_QUALITY,
                AgentRunState.CHECKING_CONTINUITY);
        repository.transition(
                run.runId(),
                AgentRunState.REVIEWING_QUALITY,
                AgentRunState.CHECKING_CONTINUITY,
                AgentStepType.CHECK_CONTINUITY);
        started = System.nanoTime();
        ReviewReport continuityReport = modelProvider.reviewContinuity(draft);
        String continuityStepId = recordModelStep(
                run.runId(),
                AgentStepType.CHECK_CONTINUITY,
                "chapter:" + chapterNo,
                draft.revisionNo(),
                draft,
                continuityReport,
                started);

        if (!continuityReport.passed()) {
            if (continuityReport.requiresUserDecision()) {
                pauseForDecision(
                        getRun(run.runId()),
                        continuityStepId,
                        AgentRunState.CHECKING_CONTINUITY,
                        firstIssueCode(continuityReport),
                        "章节连续性问题需要用户决策",
                        firstIssueMessage(continuityReport));
                return;
            }
            throw new NovelAgentConflictException("连续性检查未通过且没有可执行决策");
        }

        stateMachine.requireTransition(
                AgentRunState.CHECKING_CONTINUITY,
                AgentRunState.COMMITTING_CHAPTER);
        repository.transition(
                run.runId(),
                AgentRunState.CHECKING_CONTINUITY,
                AgentRunState.COMMITTING_CHAPTER,
                AgentStepType.COMMIT_CHAPTER);
        repository.commitChapter(
                getRun(run.runId()),
                plan,
                draft,
                qualityReport,
                continuityReport);
        repository.recordStepSuccess(
                run.runId(),
                AgentStepType.COMMIT_CHAPTER,
                "chapter:" + chapterNo,
                draft.revisionNo(),
                java.util.Map.of("chapterNo", chapterNo),
                java.util.Map.of("committed", true));
    }

    private ReviewReport reviewQuality(
            AgentRunSnapshot run,
            ChapterDraft draft) {
        long started = System.nanoTime();
        ReviewReport report = modelProvider.reviewQuality(draft);
        recordModelStep(
                run.runId(),
                AgentStepType.REVIEW_QUALITY,
                "chapter:" + draft.chapterNo(),
                draft.revisionNo(),
                draft,
                report,
                started);
        return report;
    }

    private void pauseForDecision(
            AgentRunSnapshot run,
            String triggeringStepId,
            AgentRunState expectedState,
            String decisionType,
            String title,
            String context) {
        stateMachine.requireTransition(expectedState, AgentRunState.WAITING_USER_DECISION);
        repository.createDecisionAndPause(
                run,
                triggeringStepId,
                expectedState,
                decisionType,
                title,
                context,
                List.of(
                        java.util.Map.of("code", "REVISE_DIRECTION", "label", "调整方向后重写"),
                        java.util.Map.of("code", "ACCEPT_CHANGE", "label", "接受重大变化")),
                java.util.Map.of(
                        "code", "REVISE_DIRECTION",
                        "reason", "优先保持已经确认的创意和正式事实"),
                java.util.Map.of("chapterNo", run.currentChapterNo() + 1));
    }

    private String recordModelStep(
            String runId,
            AgentStepType stepType,
            String subjectKey,
            int revisionNo,
            Object input,
            Object output,
            long startedNanos) {
        String stepId = repository.recordStepSuccess(
                runId,
                stepType,
                subjectKey,
                revisionNo,
                input,
                output);
        long inputTokens = estimateTokens(input);
        long outputTokens = estimateTokens(output);
        repository.recordModelCallAndUsage(
                runId,
                stepId,
                modelProvider.providerName(),
                modelProvider.modelName(),
                stepType,
                inputTokens,
                outputTokens,
                BigDecimal.ZERO,
                Duration.ofNanos(System.nanoTime() - startedNanos).toMillis());
        return stepId;
    }

    private boolean budgetExhausted(AgentRunSnapshot run) {
        if (run.tokenBudget() > 0 && run.tokenUsed() >= run.tokenBudget()) {
            return true;
        }
        if (run.costBudget().signum() > 0
                && run.costUsed().compareTo(run.costBudget()) >= 0) {
            return true;
        }
        return run.startedAt() != null
                && Duration.between(run.startedAt(), LocalDateTime.now()).getSeconds()
                >= run.timeBudgetSeconds();
    }

    private static void validateProposals(List<CreativeProposalDraft> proposals) {
        if (proposals.size() != 3) {
            throw new IllegalStateException("第一阶段必须生成三套创意");
        }
        Set<Integer> numbers = new HashSet<>();
        Set<String> titles = new HashSet<>();
        for (CreativeProposalDraft proposal : proposals) {
            numbers.add(proposal.proposalNo());
            titles.add(proposal.title());
        }
        if (!numbers.equals(Set.of(1, 2, 3)) || titles.size() != 3) {
            throw new IllegalStateException("三套创意的编号或标题不符合要求");
        }
    }

    private static void validateFoundation(FoundationDraft foundation) {
        if (foundation.novelTitle() == null
                || foundation.novelTitle().isBlank()
                || foundation.world().isEmpty()
                || foundation.characters().isEmpty()
                || foundation.stages().size() != 10
                || foundation.storyBibleFacts().isEmpty()) {
            throw new IllegalStateException("小说初始化结果不完整");
        }
    }

    private static void validatePlans(List<ChapterPlanDraft> plans) {
        if (plans.size() != 10) {
            throw new IllegalStateException("第一阶段初始化必须生成前10章章节卡");
        }
        for (int index = 0; index < plans.size(); index++) {
            if (plans.get(index).chapterNo() != index + 1) {
                throw new IllegalStateException("章节卡编号不连续");
            }
        }
    }

    private static void validateChapter(ChapterDraft draft, int expectedChapterNo) {
        if (draft.chapterNo() != expectedChapterNo) {
            throw new IllegalStateException("模型返回的章节号错误");
        }
        if (draft.title() == null
                || draft.title().isBlank()
                || draft.content() == null
                || draft.content().isBlank()
                || draft.summary() == null
                || draft.summary().isBlank()
                || draft.metadata() == null
                || draft.metadata().isEmpty()) {
            throw new IllegalStateException("章节正文或结构化元数据不完整");
        }
    }

    private static String firstIssueCode(ReviewReport report) {
        return report.issues().stream()
                .findFirst()
                .map(ReviewIssue::code)
                .orElse("UNKNOWN_REVIEW_ISSUE");
    }

    private static String firstIssueMessage(ReviewReport report) {
        return report.issues().stream()
                .findFirst()
                .map(ReviewIssue::message)
                .orElse("检查未通过");
    }

    private static long estimateTokens(Object value) {
        return Math.max(1, String.valueOf(value).length() / 4L);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

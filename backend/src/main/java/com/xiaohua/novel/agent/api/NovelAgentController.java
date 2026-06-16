package com.xiaohua.novel.agent.api;

import java.net.URI;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohua.novel.agent.application.CreateNovelRunCommand;
import com.xiaohua.novel.agent.application.NovelAgentService;
import com.xiaohua.novel.agent.domain.AgentRunSnapshot;
import com.xiaohua.novel.shared.api.ApiPaths;
import com.xiaohua.novel.shared.config.OpenApiConfig;
import com.xiaohua.novel.shared.security.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.NOVEL_AGENT)
@Tag(name = "小说总导演", description = "创建和查看自主小说创作运行，用户只处理创意选择与关键决策")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class NovelAgentController {

    private final NovelAgentService novelAgentService;
    private final ObjectMapper objectMapper;

    public NovelAgentController(
            NovelAgentService novelAgentService,
            ObjectMapper objectMapper) {
        this.novelAgentService = novelAgentService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/runs")
    @Operation(
            summary = "输入想法并生成三套创意",
            description = """
                    创建一次 NovelAgent 运行，系统自动完成第一轮研究并生成三套差异明显的创意。
                    返回后运行会停在 WAITING_CREATIVE_SELECTION，等待用户选择方向。
                    当前使用 Fake Provider 验证流程，不代表真实模型最终内容质量。
                    """)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "运行创建成功，三套创意已生成"),
        @ApiResponse(
                responseCode = "400",
                description = "请求参数不合法",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "未携带有效访问令牌",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<NovelAgentRunResponse> createRun(
            @Valid @RequestBody CreateNovelRunRequest request) {
        AgentRunSnapshot run = novelAgentService.createRun(new CreateNovelRunCommand(
                request.idea(),
                request.genre(),
                request.targetChapters(),
                request.tokenBudget(),
                request.costBudget(),
                request.timeBudgetSeconds()));
        return ResponseEntity
                .created(URI.create("/api/novel-agent/runs/" + run.runId()))
                .body(toResponse(run));
    }

    @GetMapping("/runs/{runId}")
    @Operation(
            summary = "查看小说智能体运行进度",
            description = "返回状态机状态、预算、三套创意、正式章节、检查点和待决策事项。")
    @ApiResponse(responseCode = "200", description = "运行状态查询成功")
    public NovelAgentRunResponse getRun(@PathVariable String runId) {
        return toResponse(novelAgentService.getRun(runId));
    }

    @PostMapping("/runs/{runId}/creative-selection")
    @Operation(
            summary = "选择创意并自动完成三章原型",
            description = """
                    选择三套创意中的一个方向。系统随后自动初始化人物、世界、主线、阶段、
                    伏笔和前10章章节卡，再逐章生成、检查、自动修订并提交正文，
                    直到达到本次目标章节数或遇到关键决策、预算和错误而安全暂停。
                    """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创意已选择，自动流程已执行到终态或暂停状态"),
        @ApiResponse(
                responseCode = "404",
                description = "运行或创意不存在",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "运行当前状态不允许选择创意",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public NovelAgentRunResponse selectCreative(
            @PathVariable String runId,
            @Valid @RequestBody CreativeSelectionRequest request) {
        return toResponse(novelAgentService.selectCreative(
                runId,
                request.proposalId(),
                request.adjustments()));
    }

    @GetMapping("/runs/{runId}/chapters")
    @Operation(
            summary = "查看已经正式提交的章节",
            description = "只返回通过格式、质量和连续性检查并完成事务提交的章节，不返回未通过检查的草稿。")
    @ApiResponse(responseCode = "200", description = "正式章节查询成功")
    public List<ChapterResponse> getChapters(@PathVariable String runId) {
        return novelAgentService.getChapters(runId).stream()
                .map(ChapterResponse::from)
                .toList();
    }

    @GetMapping("/runs/{runId}/decisions")
    @Operation(
            summary = "查看需要用户处理的关键决策",
            description = "返回重要人物命运、主线、关系、世界规则或修订上限等阻塞自动运行的事项。")
    @ApiResponse(responseCode = "200", description = "待决策事项查询成功")
    public List<DecisionResponse> getDecisions(@PathVariable String runId) {
        return novelAgentService.getOpenDecisions(runId).stream()
                .map(decision -> DecisionResponse.from(decision, objectMapper))
                .toList();
    }

    @PostMapping("/runs/{runId}/decisions/{decisionId}/resolve")
    @Operation(
            summary = "处理关键决策并恢复自动写作",
            description = """
                    提交用户对重要人物命运、主线或连续性冲突等事项的选择。
                    系统保存决定、废弃受影响草稿，从安全检查点恢复并继续写到本次目标章节。
                    """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "决策已处理，自动运行已恢复"),
        @ApiResponse(
                responseCode = "409",
                description = "运行不在等待决策状态，或决策已经处理",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public NovelAgentRunResponse resolveDecision(
            @PathVariable String runId,
            @PathVariable String decisionId,
            @Valid @RequestBody DecisionResolutionRequest request) {
        return toResponse(novelAgentService.resolveDecision(
                runId,
                decisionId,
                request.optionCode(),
                request.comment()));
    }

    @PostMapping("/runs/{runId}/resume")
    @Operation(
            summary = "提高预算并恢复自动写作",
            description = """
                    当运行因为Token、费用或时间预算进入 PAUSED_BY_BUDGET 后，
                    可以提高总预算并从最近正式检查点继续。未提交草稿不会被当作正式事实。
                    """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "运行已恢复并继续执行"),
        @ApiResponse(
                responseCode = "409",
                description = "运行当前不是可恢复的暂停状态",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public NovelAgentRunResponse resumeRun(
            @PathVariable String runId,
            @Valid @RequestBody ResumeNovelRunRequest request) {
        return toResponse(novelAgentService.resumeRun(
                runId,
                request.tokenBudget(),
                request.costBudget(),
                request.timeBudgetSeconds()));
    }

    private NovelAgentRunResponse toResponse(AgentRunSnapshot run) {
        List<CreativeProposalResponse> proposals = novelAgentService
                .getProposals(run.runId())
                .stream()
                .map(CreativeProposalResponse::from)
                .toList();
        List<ChapterResponse> chapters = novelAgentService
                .getChapters(run.runId())
                .stream()
                .map(ChapterResponse::from)
                .toList();
        List<DecisionResponse> decisions = novelAgentService
                .getOpenDecisions(run.runId())
                .stream()
                .map(decision -> DecisionResponse.from(decision, objectMapper))
                .toList();
        return new NovelAgentRunResponse(
                run.runId(),
                run.projectId(),
                run.novelTitle(),
                run.state(),
                run.currentStep(),
                run.currentChapterNo(),
                run.targetChapterNo(),
                run.tokenBudget(),
                run.tokenUsed(),
                run.costBudget(),
                run.costUsed(),
                run.lastCheckpoint(),
                proposals,
                chapters,
                decisions,
                run.updatedAt());
    }
}

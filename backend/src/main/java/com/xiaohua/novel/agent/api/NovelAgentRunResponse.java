package com.xiaohua.novel.agent.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.xiaohua.novel.agent.domain.AgentRunState;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "NovelAgent 当前运行状态和正式产出")
public record NovelAgentRunResponse(
        @Schema(description = "运行ID")
        String runId,
        @Schema(description = "小说项目ID")
        String projectId,
        @Schema(description = "小说标题，选择创意并初始化后生成")
        String novelTitle,
        @Schema(description = "NovelAgent 持久化状态")
        AgentRunState state,
        @Schema(description = "当前执行步骤")
        String currentStep,
        @Schema(description = "已经正式提交的章节数")
        int currentChapterNo,
        @Schema(description = "本次运行目标章节数")
        int targetChapterNo,
        @Schema(description = "Token预算")
        long tokenBudget,
        @Schema(description = "已使用Token估算值")
        long tokenUsed,
        @Schema(description = "费用预算")
        BigDecimal costBudget,
        @Schema(description = "已使用费用")
        BigDecimal costUsed,
        @Schema(description = "最后一个安全检查点")
        String lastCheckpoint,
        @Schema(description = "三套创意候选")
        List<CreativeProposalResponse> proposals,
        @Schema(description = "已经正式提交的章节")
        List<ChapterResponse> chapters,
        @Schema(description = "当前阻塞运行的待决策事项")
        List<DecisionResponse> openDecisions,
        @Schema(description = "运行更新时间")
        LocalDateTime updatedAt) {
}

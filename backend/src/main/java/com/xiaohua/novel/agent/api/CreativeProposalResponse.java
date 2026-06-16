package com.xiaohua.novel.agent.api;

import com.xiaohua.novel.agent.domain.CreativeProposalRecord;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "NovelAgent 生成的一套创意方向")
public record CreativeProposalResponse(
        @Schema(description = "创意方案ID")
        String id,
        @Schema(description = "方案序号", example = "1")
        int proposalNo,
        @Schema(description = "建议书名")
        String title,
        @Schema(description = "核心故事设定")
        String premise,
        @Schema(description = "目标读者")
        String targetAudience,
        @Schema(description = "市场与创作依据")
        String marketRationale,
        @Schema(description = "与常见方案的差异")
        String differentiator,
        @Schema(description = "主要创作风险")
        String riskSummary,
        @Schema(description = "是否已被用户选中")
        boolean selected) {

    public static CreativeProposalResponse from(CreativeProposalRecord proposal) {
        return new CreativeProposalResponse(
                proposal.id(),
                proposal.proposalNo(),
                proposal.title(),
                proposal.premise(),
                proposal.targetAudience(),
                proposal.marketRationale(),
                proposal.differentiator(),
                proposal.riskSummary(),
                proposal.selected());
    }
}

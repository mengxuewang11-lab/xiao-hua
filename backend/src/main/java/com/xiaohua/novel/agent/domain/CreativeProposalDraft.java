package com.xiaohua.novel.agent.domain;

import java.util.Map;

public record CreativeProposalDraft(
        int proposalNo,
        String title,
        String premise,
        String targetAudience,
        String marketRationale,
        String differentiator,
        String riskSummary,
        Map<String, Object> details) {
}

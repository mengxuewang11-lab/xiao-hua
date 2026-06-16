package com.xiaohua.novel.agent.domain;

public record CreativeProposalRecord(
        String id,
        String runId,
        String projectId,
        int proposalNo,
        String title,
        String premise,
        String targetAudience,
        String marketRationale,
        String differentiator,
        String riskSummary,
        boolean selected) {
}

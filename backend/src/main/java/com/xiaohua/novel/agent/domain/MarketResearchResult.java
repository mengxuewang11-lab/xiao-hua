package com.xiaohua.novel.agent.domain;

import java.util.List;

public record MarketResearchResult(
        String targetAudience,
        List<String> opportunities,
        List<String> risks,
        List<String> recommendations) {
}

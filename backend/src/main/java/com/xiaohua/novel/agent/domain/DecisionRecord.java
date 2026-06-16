package com.xiaohua.novel.agent.domain;

import java.time.LocalDateTime;

public record DecisionRecord(
        String id,
        String decisionType,
        String priority,
        String status,
        String title,
        String contextText,
        String optionsJson,
        String recommendationJson,
        LocalDateTime createdAt) {
}

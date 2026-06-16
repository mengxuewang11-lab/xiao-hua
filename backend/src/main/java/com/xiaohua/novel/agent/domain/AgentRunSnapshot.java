package com.xiaohua.novel.agent.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AgentRunSnapshot(
        String runId,
        String projectId,
        String originalIdea,
        String genre,
        String novelTitle,
        String projectStatus,
        AgentRunState state,
        String currentStep,
        int currentChapterNo,
        int targetChapterNo,
        long tokenBudget,
        long tokenUsed,
        BigDecimal costBudget,
        BigDecimal costUsed,
        int timeBudgetSeconds,
        String lastCheckpoint,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime updatedAt) {
}

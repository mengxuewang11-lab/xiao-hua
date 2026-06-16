package com.xiaohua.novel.agent.application;

import java.math.BigDecimal;

public record CreateNovelRunCommand(
        String idea,
        String genre,
        Integer targetChapters,
        Long tokenBudget,
        BigDecimal costBudget,
        Integer timeBudgetSeconds) {
}

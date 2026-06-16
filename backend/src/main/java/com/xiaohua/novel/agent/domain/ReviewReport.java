package com.xiaohua.novel.agent.domain;

import java.util.List;

public record ReviewReport(
        boolean passed,
        List<ReviewIssue> issues) {

    public boolean requiresUserDecision() {
        return issues.stream().anyMatch(issue -> !issue.autoFixable());
    }

    public static ReviewReport success() {
        return new ReviewReport(true, List.of());
    }
}

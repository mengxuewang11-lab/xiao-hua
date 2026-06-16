package com.xiaohua.novel.agent.domain;

public record ReviewIssue(
        String code,
        String severity,
        String message,
        boolean autoFixable) {
}

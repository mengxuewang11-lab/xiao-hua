package com.xiaohua.novel.agent.domain;

public record ChapterPlanRecord(
        String id,
        String projectId,
        int chapterNo,
        String title,
        String objective,
        String conflictSummary,
        String climax,
        String endingHook) {
}

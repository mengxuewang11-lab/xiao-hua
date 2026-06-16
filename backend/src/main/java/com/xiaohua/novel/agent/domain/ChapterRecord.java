package com.xiaohua.novel.agent.domain;

public record ChapterRecord(
        String id,
        int chapterNo,
        String title,
        String content,
        String summary,
        int wordCount,
        int versionNo) {
}

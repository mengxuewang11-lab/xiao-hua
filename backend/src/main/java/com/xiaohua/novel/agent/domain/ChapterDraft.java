package com.xiaohua.novel.agent.domain;

import java.util.Map;

public record ChapterDraft(
        int chapterNo,
        String title,
        String content,
        String summary,
        Map<String, Object> metadata,
        int revisionNo) {

    public ChapterDraft revised(String revisedContent, int nextRevisionNo) {
        return new ChapterDraft(
                chapterNo,
                title,
                revisedContent,
                summary,
                metadata,
                nextRevisionNo);
    }
}

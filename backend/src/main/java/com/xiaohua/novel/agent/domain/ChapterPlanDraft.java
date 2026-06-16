package com.xiaohua.novel.agent.domain;

import java.util.List;
import java.util.Map;

public record ChapterPlanDraft(
        int chapterNo,
        String title,
        String objective,
        String conflictSummary,
        List<String> keyEvents,
        List<Map<String, Object>> characterChanges,
        List<Map<String, Object>> foreshadowingActions,
        String climax,
        String endingHook,
        List<String> forbiddenEvents) {
}

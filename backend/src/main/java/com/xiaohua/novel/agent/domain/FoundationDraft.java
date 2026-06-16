package com.xiaohua.novel.agent.domain;

import java.util.List;
import java.util.Map;

public record FoundationDraft(
        String novelTitle,
        Map<String, Object> world,
        List<Map<String, Object>> characters,
        List<Map<String, Object>> relationships,
        Map<String, Object> mainPlot,
        List<Map<String, Object>> stages,
        List<Map<String, Object>> foreshadowing,
        List<Map<String, Object>> storyBibleFacts) {
}

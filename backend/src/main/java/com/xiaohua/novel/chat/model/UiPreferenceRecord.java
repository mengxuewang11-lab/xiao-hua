package com.xiaohua.novel.chat.model;

public record UiPreferenceRecord(
        String userId,
        String themeId,
        String customAccentColor,
        boolean sidebarCollapsed,
        String defaultProvider,
        String defaultModel) {
}

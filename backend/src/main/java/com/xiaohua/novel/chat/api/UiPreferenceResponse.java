package com.xiaohua.novel.chat.api;

import com.xiaohua.novel.chat.model.UiPreferenceRecord;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户 Web 工作台偏好设置")
public record UiPreferenceResponse(
        @Schema(description = "主题编码", example = "peach")
        String themeId,
        @Schema(description = "自定义主题色", example = "#ff6ea8")
        String customAccentColor,
        @Schema(description = "侧边栏是否折叠")
        boolean sidebarCollapsed,
        @Schema(description = "默认模型供应商", example = "zhipu")
        String defaultProvider,
        @Schema(description = "默认模型编码", example = "glm-4.7-flash")
        String defaultModel) {

    public static UiPreferenceResponse from(UiPreferenceRecord preference) {
        return new UiPreferenceResponse(
                preference.themeId(),
                preference.customAccentColor(),
                preference.sidebarCollapsed(),
                preference.defaultProvider(),
                preference.defaultModel());
    }
}

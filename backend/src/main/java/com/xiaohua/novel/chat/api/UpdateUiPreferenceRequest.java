package com.xiaohua.novel.chat.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "更新 Web 工作台偏好设置请求")
public record UpdateUiPreferenceRequest(
        @Schema(description = "主题编码", example = "peach")
        @NotBlank(message = "主题编码不能为空")
        @Size(max = 40, message = "主题编码不能超过40个字符")
        String themeId,

        @Schema(description = "自定义主题色，十六进制颜色", example = "#ff6ea8")
        @NotBlank(message = "自定义主题色不能为空")
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "自定义主题色必须是 #RRGGBB 格式")
        String customAccentColor,

        @Schema(description = "侧边栏是否折叠")
        boolean sidebarCollapsed,

        @Schema(description = "默认模型供应商", example = "zhipu")
        @NotBlank(message = "默认模型供应商不能为空")
        @Size(max = 40, message = "默认模型供应商不能超过40个字符")
        String defaultProvider,

        @Schema(description = "默认模型编码", example = "glm-4.7-flash")
        @NotBlank(message = "默认模型编码不能为空")
        @Size(max = 120, message = "默认模型编码不能超过120个字符")
        String defaultModel) {
}

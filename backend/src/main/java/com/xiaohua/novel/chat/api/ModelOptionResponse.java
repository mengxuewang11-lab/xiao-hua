package com.xiaohua.novel.chat.api;

import com.xiaohua.novel.chat.model.ModelOption;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "前端可选择的大模型")
public record ModelOptionResponse(
        @Schema(description = "模型供应商编码", example = "zhipu")
        String provider,
        @Schema(description = "模型供应商显示名称", example = "智谱 GLM")
        String providerName,
        @Schema(description = "模型编码", example = "glm-4.7-flash")
        String model,
        @Schema(description = "模型显示名称", example = "GLM-4.7-Flash")
        String displayName,
        @Schema(description = "是否官方免费模型")
        boolean freeTier,
        @Schema(description = "当前后端是否已经配置 API Key，可直接调用")
        boolean enabled,
        @Schema(description = "模型用途说明")
        String description) {

    public static ModelOptionResponse from(ModelOption option) {
        return new ModelOptionResponse(
                option.provider(),
                option.providerName(),
                option.model(),
                option.displayName(),
                option.freeTier(),
                option.enabled(),
                option.description());
    }
}

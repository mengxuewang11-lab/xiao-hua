package com.xiaohua.novel.chat.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "创建聊天会话请求")
public record CreateConversationRequest(
        @Schema(description = "会话标题，不填则自动生成", example = "旧物修理师")
        @Size(max = 200, message = "会话标题不能超过200个字符")
        String title,

        @Schema(description = "模型供应商，不填使用当前偏好", example = "zhipu")
        @Size(max = 40, message = "模型供应商不能超过40个字符")
        String provider,

        @Schema(description = "模型编码，不填使用当前偏好", example = "glm-4.7-flash")
        @Size(max = 120, message = "模型编码不能超过120个字符")
        String model) {
}

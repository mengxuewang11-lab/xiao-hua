package com.xiaohua.novel.chat.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "发送聊天消息请求")
public record SendChatMessageRequest(
        @Schema(description = "会话ID；为空时自动新建会话")
        @Size(max = 36, message = "会话ID不能超过36个字符")
        String conversationId,

        @Schema(description = "用户输入内容", example = "帮我设计一个旧物修理师小说开局")
        @NotBlank(message = "消息内容不能为空")
        @Size(max = 8000, message = "消息内容不能超过8000个字符")
        String content,

        @Schema(description = "本次消息使用的模型供应商，不填使用当前偏好", example = "zhipu")
        @Size(max = 40, message = "模型供应商不能超过40个字符")
        String provider,

        @Schema(description = "本次消息使用的模型编码，不填使用当前偏好", example = "glm-4.7-flash")
        @Size(max = 120, message = "模型编码不能超过120个字符")
        String model) {
}

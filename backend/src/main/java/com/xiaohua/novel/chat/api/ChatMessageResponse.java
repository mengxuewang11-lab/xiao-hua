package com.xiaohua.novel.chat.api;

import java.time.LocalDateTime;

import com.xiaohua.novel.chat.model.ChatMessageRecord;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "聊天消息")
public record ChatMessageResponse(
        @Schema(description = "消息ID")
        String id,
        @Schema(description = "会话ID")
        String conversationId,
        @Schema(description = "角色：user 或 assistant", example = "assistant")
        String role,
        @Schema(description = "消息正文")
        String content,
        @Schema(description = "模型供应商")
        String provider,
        @Schema(description = "模型编码")
        String modelName,
        @Schema(description = "输入 token 数")
        long promptTokens,
        @Schema(description = "输出 token 数")
        long completionTokens,
        @Schema(description = "总 token 数")
        long totalTokens,
        @Schema(description = "创建时间")
        LocalDateTime createdAt) {

    public static ChatMessageResponse from(ChatMessageRecord message) {
        return new ChatMessageResponse(
                message.id(),
                message.conversationId(),
                message.role(),
                message.content(),
                message.provider(),
                message.modelName(),
                message.promptTokens(),
                message.completionTokens(),
                message.totalTokens(),
                message.createdAt());
    }
}

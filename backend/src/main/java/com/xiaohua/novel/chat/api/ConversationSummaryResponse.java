package com.xiaohua.novel.chat.api;

import java.time.LocalDateTime;

import com.xiaohua.novel.chat.model.ChatConversationRecord;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "聊天会话摘要")
public record ConversationSummaryResponse(
        @Schema(description = "会话ID")
        String id,
        @Schema(description = "会话标题")
        String title,
        @Schema(description = "模型供应商")
        String provider,
        @Schema(description = "模型编码")
        String modelName,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt) {

    public static ConversationSummaryResponse from(ChatConversationRecord conversation) {
        return new ConversationSummaryResponse(
                conversation.id(),
                conversation.title(),
                conversation.provider(),
                conversation.modelName(),
                conversation.createdAt(),
                conversation.updatedAt());
    }
}

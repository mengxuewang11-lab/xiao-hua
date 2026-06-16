package com.xiaohua.novel.chat.api;

import java.util.List;

import com.xiaohua.novel.chat.model.ChatConversationRecord;
import com.xiaohua.novel.chat.model.ChatMessageRecord;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "聊天会话详情")
public record ConversationDetailResponse(
        @Schema(description = "会话摘要")
        ConversationSummaryResponse conversation,
        @Schema(description = "会话消息列表")
        List<ChatMessageResponse> messages) {

    public static ConversationDetailResponse from(
            ChatConversationRecord conversation,
            List<ChatMessageRecord> messages) {
        return new ConversationDetailResponse(
                ConversationSummaryResponse.from(conversation),
                messages.stream().map(ChatMessageResponse::from).toList());
    }
}

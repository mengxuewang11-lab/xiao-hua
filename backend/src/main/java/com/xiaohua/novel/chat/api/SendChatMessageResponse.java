package com.xiaohua.novel.chat.api;

import com.xiaohua.novel.chat.model.ChatConversationRecord;
import com.xiaohua.novel.chat.model.ChatMessageRecord;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "发送聊天消息后的响应")
public record SendChatMessageResponse(
        @Schema(description = "当前会话")
        ConversationSummaryResponse conversation,
        @Schema(description = "刚保存的用户消息")
        ChatMessageResponse userMessage,
        @Schema(description = "模型生成并保存的助手回复")
        ChatMessageResponse assistantMessage) {

    public static SendChatMessageResponse from(
            ChatConversationRecord conversation,
            ChatMessageRecord userMessage,
            ChatMessageRecord assistantMessage) {
        return new SendChatMessageResponse(
                ConversationSummaryResponse.from(conversation),
                ChatMessageResponse.from(userMessage),
                ChatMessageResponse.from(assistantMessage));
    }
}

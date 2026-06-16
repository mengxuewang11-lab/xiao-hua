package com.xiaohua.novel.chat.model;

import java.time.LocalDateTime;

public record ChatMessageRecord(
        String id,
        String conversationId,
        String role,
        String content,
        String provider,
        String modelName,
        long promptTokens,
        long completionTokens,
        long totalTokens,
        String errorCode,
        String errorMessage,
        LocalDateTime createdAt) {
}

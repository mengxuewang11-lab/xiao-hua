package com.xiaohua.novel.chat.model;

import java.time.LocalDateTime;

public record ChatConversationRecord(
        String id,
        String userId,
        String title,
        String provider,
        String modelName,
        boolean archived,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

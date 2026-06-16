package com.xiaohua.novel.chat.model;

public record ModelChatResponse(
        String provider,
        String model,
        String content,
        long promptTokens,
        long completionTokens,
        long totalTokens,
        long durationMs) {
}

package com.xiaohua.novel.chat.model;

import java.util.List;

public record ModelChatRequest(
        String provider,
        String model,
        List<ModelChatMessage> messages) {
}

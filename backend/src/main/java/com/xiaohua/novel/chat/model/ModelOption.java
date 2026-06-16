package com.xiaohua.novel.chat.model;

public record ModelOption(
        String provider,
        String providerName,
        String model,
        String displayName,
        boolean freeTier,
        boolean enabled,
        String description) {
}

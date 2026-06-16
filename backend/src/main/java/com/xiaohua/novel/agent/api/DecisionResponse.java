package com.xiaohua.novel.agent.api;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohua.novel.agent.domain.DecisionRecord;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "需要用户处理后才能继续的关键决策")
public record DecisionResponse(
        @Schema(description = "决策ID")
        String id,
        @Schema(description = "决策类型")
        String decisionType,
        @Schema(description = "优先级")
        String priority,
        @Schema(description = "状态")
        String status,
        @Schema(description = "决策标题")
        String title,
        @Schema(description = "触发原因和背景")
        String context,
        @Schema(description = "可选方案")
        JsonNode options,
        @Schema(description = "系统建议")
        JsonNode recommendation,
        @Schema(description = "创建时间")
        LocalDateTime createdAt) {

    public static DecisionResponse from(
            DecisionRecord decision,
            ObjectMapper objectMapper) {
        return new DecisionResponse(
                decision.id(),
                decision.decisionType(),
                decision.priority(),
                decision.status(),
                decision.title(),
                decision.contextText(),
                readTree(objectMapper, decision.optionsJson()),
                readTree(objectMapper, decision.recommendationJson()),
                decision.createdAt());
    }

    private static JsonNode readTree(ObjectMapper objectMapper, String json) {
        try {
            return json == null ? objectMapper.nullNode() : objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new IllegalStateException("无法读取决策JSON", exception);
        }
    }
}

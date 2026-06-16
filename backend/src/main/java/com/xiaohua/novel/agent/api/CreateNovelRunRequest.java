package com.xiaohua.novel.agent.api;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "创建 NovelAgent 自动创作运行的请求")
public record CreateNovelRunRequest(
        @Schema(
                description = "小说初始想法，系统会围绕它完成研究和三套创意提案",
                example = "一个普通维修工发现每次修好旧物，都能看见物品主人遗失的一段记忆")
        @NotBlank(message = "小说想法不能为空")
        @Size(min = 5, max = 2000, message = "小说想法长度必须在5到2000字之间")
        String idea,

        @Schema(description = "期望题材，可为空", example = "都市悬疑")
        @Size(max = 100, message = "题材不能超过100字")
        String genre,

        @Schema(description = "本次自动运行目标章节数，第一阶段最多3章", example = "3")
        @Min(value = 1, message = "目标章节数不能小于1")
        @Max(value = 3, message = "第一阶段目标章节数不能超过3")
        Integer targetChapters,

        @Schema(description = "Token预算，达到后安全暂停；不填默认100000", example = "100000")
        @Min(value = 1, message = "Token预算必须大于0")
        Long tokenBudget,

        @Schema(description = "费用预算，Fake Provider阶段可以为0", example = "0")
        @DecimalMin(value = "0", message = "费用预算不能为负数")
        BigDecimal costBudget,

        @Schema(description = "最长自动运行秒数，不填默认3600秒", example = "3600")
        @Min(value = 60, message = "运行时间预算不能小于60秒")
        @Max(value = 86400, message = "运行时间预算不能超过86400秒")
        Integer timeBudgetSeconds) {
}

package com.xiaohua.novel.agent.api;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "提高运行预算并从安全检查点恢复的请求")
public record ResumeNovelRunRequest(
        @Schema(description = "新的Token总预算，不填则保持不变", example = "100000")
        @Min(value = 1, message = "Token预算必须大于0")
        Long tokenBudget,

        @Schema(description = "新的费用总预算，不填则保持不变", example = "10")
        @DecimalMin(value = "0", message = "费用预算不能为负数")
        BigDecimal costBudget,

        @Schema(description = "新的运行时间总预算秒数，不填则保持不变", example = "3600")
        @Min(value = 60, message = "运行时间预算不能小于60秒")
        @Max(value = 86400, message = "运行时间预算不能超过86400秒")
        Integer timeBudgetSeconds) {
}

package com.xiaohua.novel.agent.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "处理关键决策并恢复 NovelAgent 的请求")
public record DecisionResolutionRequest(
        @Schema(
                description = "用户选择的方案代码",
                example = "REVISE_DIRECTION")
        @NotBlank(message = "必须选择一个决策方案")
        String optionCode,

        @Schema(description = "用户补充说明，可为空")
        @Size(max = 2000, message = "补充说明不能超过2000字")
        String comment) {
}

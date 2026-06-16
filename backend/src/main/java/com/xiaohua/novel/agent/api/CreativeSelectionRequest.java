package com.xiaohua.novel.agent.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "选择创意并启动自动初始化和连续写作的请求")
public record CreativeSelectionRequest(
        @Schema(description = "从三套创意中选中的方案ID")
        @NotBlank(message = "必须选择一个创意方案")
        String proposalId,

        @Schema(
                description = "用户对所选方向的补充要求，可为空",
                example = "感情线慢热，不要让主角一开始就完全信任搭档")
        @Size(max = 2000, message = "补充要求不能超过2000字")
        String adjustments) {
}

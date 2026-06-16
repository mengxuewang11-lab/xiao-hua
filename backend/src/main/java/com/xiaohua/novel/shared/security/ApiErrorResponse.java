package com.xiaohua.novel.shared.security;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "接口错误响应")
public record ApiErrorResponse(
        @Schema(description = "HTTP 状态码", example = "401")
        int status,
        @Schema(description = "稳定的业务错误码", example = "UNAUTHORIZED")
        String code,
        @Schema(description = "中文错误说明", example = "缺少或无效的访问令牌")
        String message,
        @Schema(description = "发生错误的接口路径", example = "/api/test/database")
        String path,
        @Schema(description = "错误发生时间")
        OffsetDateTime timestamp) {
}

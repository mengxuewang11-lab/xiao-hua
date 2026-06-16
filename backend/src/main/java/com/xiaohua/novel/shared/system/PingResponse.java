package com.xiaohua.novel.shared.system;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "后端服务连通性响应")
public record PingResponse(
        @Schema(description = "服务状态", example = "UP")
        String status,
        @Schema(description = "应用名称", example = "novel-backend")
        String application,
        @Schema(description = "后端当前时间")
        OffsetDateTime timestamp) {
}

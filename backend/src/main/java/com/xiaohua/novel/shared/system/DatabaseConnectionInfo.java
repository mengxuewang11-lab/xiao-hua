package com.xiaohua.novel.shared.system;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "数据库连接与迁移状态")
public record DatabaseConnectionInfo(
        @Schema(description = "数据库连接状态", example = "UP")
        String status,
        @Schema(description = "当前数据库名称", example = "novel_master")
        String databaseName,
        @Schema(description = "当前数据库账户", example = "novel_app@127.0.0.1")
        String currentUser,
        @Schema(description = "MySQL 版本", example = "8.0.46")
        String databaseVersion,
        @Schema(description = "数据库字符集", example = "utf8mb4")
        String characterSet,
        @Schema(description = "数据库排序规则", example = "utf8mb4_0900_ai_ci")
        String collation,
        @Schema(description = "数据库服务器时间")
        LocalDateTime serverTime,
        @Schema(description = "已成功执行的最高 Flyway 版本", example = "1")
        String flywayVersion,
        @Schema(description = "Flyway V1 创建的迁移探针", example = "Flyway migration V1 applied")
        String migrationProbe) {
}

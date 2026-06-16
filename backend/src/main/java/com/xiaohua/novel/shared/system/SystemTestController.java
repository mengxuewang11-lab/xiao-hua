package com.xiaohua.novel.shared.system;

import java.time.OffsetDateTime;

import com.xiaohua.novel.shared.api.ApiPaths;
import com.xiaohua.novel.shared.config.OpenApiConfig;
import com.xiaohua.novel.shared.security.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.TEST)
@Tag(name = "系统测试", description = "开发阶段用于检查后端、数据库、Flyway 和跨域配置的临时接口")
@ConditionalOnProperty(
        prefix = "app.test-endpoints",
        name = "enabled",
        havingValue = "true")
public class SystemTestController {

    private final DatabaseConnectionTestService databaseConnectionTestService;

    public SystemTestController(DatabaseConnectionTestService databaseConnectionTestService) {
        this.databaseConnectionTestService = databaseConnectionTestService;
    }

    @GetMapping("/ping")
    @Operation(
            summary = "测试后端服务是否可访问",
            description = "公开接口。用于前端或运维快速确认 Spring Boot 服务已经启动并能正常响应。")
    @ApiResponse(responseCode = "200", description = "后端服务正常")
    public PingResponse ping() {
        return new PingResponse("UP", "novel-backend", OffsetDateTime.now());
    }

    @GetMapping("/database")
    @Operation(
            summary = "测试数据库连接和 Flyway 迁移状态",
            description = """
                    受保护接口。检查 Spring Boot 是否能通过应用账户连接 MySQL，
                    并返回当前数据库、MySQL 版本、字符集、Flyway 版本和迁移探针。
                    仅用于开发和诊断，不应在生产环境开启。
                    """)
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "数据库连接和迁移状态正常"),
        @ApiResponse(
                responseCode = "401",
                description = "未携带令牌或令牌无效",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "已认证，但没有访问权限",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public DatabaseConnectionInfo database() {
        return databaseConnectionTestService.inspect();
    }
}

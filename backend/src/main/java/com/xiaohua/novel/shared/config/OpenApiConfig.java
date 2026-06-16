package com.xiaohua.novel.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI novelPlatformOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("小花无限小说工坊 API")
                        .version("0.1.0")
                        .description("""
                                App、用户 Web 与管理系统共用的后端接口文档。

                                业务接口统一遵循 /api/{控制器}/{接口名}。
                                例如 /api/novel-agent/runs、/api/test/database。
                                后续管理系统接口统一使用 /admin-api/{控制器}/{接口名}。

                                带锁接口需要点击右上角 Authorize，输入 Bearer Token。
                                当前开发阶段使用 APP_AUTH_TOKEN 配置的静态令牌；
                                正式阶段将替换为登录签发的 JWT，不改变客户端 Authorization 请求头格式。
                                """)
                        .contact(new Contact().name("小花无限小说工坊")))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_AUTH,
                                new SecurityScheme()
                                        .name(BEARER_AUTH)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "开发阶段输入 APP_AUTH_TOKEN；正式阶段输入登录接口签发的 JWT。")));
    }
}

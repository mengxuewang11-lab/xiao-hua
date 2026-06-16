package com.xiaohua.novel.shared.system;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

import com.xiaohua.novel.shared.config.CorsConfig;
import com.xiaohua.novel.shared.security.BearerTokenAuthenticationFilter;
import com.xiaohua.novel.shared.security.RestAccessDeniedHandler;
import com.xiaohua.novel.shared.security.RestAuthenticationEntryPoint;
import com.xiaohua.novel.shared.security.SecurityConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = SystemTestController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({
    CorsConfig.class,
    SecurityConfig.class,
    BearerTokenAuthenticationFilter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class
})
@TestPropertySource(properties = {
    "app.test-endpoints.enabled=true",
    "app.security.auth-enabled=true",
    "app.security.development-token=test-development-token"
})
class SystemTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DatabaseConnectionTestService databaseConnectionTestService;

    @Test
    void pingAllowsConfiguredWebOrigin() throws Exception {
        mockMvc.perform(get("/api/test/ping")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void preflightAllowsViteDevelopmentOrigin() throws Exception {
        mockMvc.perform(options("/api/test/database")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS"));
    }

    @Test
    void databaseRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/test/database"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("缺少或无效的访问令牌"));
    }

    @Test
    void databaseAcceptsValidBearerToken() throws Exception {
        when(databaseConnectionTestService.inspect()).thenReturn(
                new DatabaseConnectionInfo(
                        "UP",
                        "novel_master",
                        "novel_app@127.0.0.1",
                        "8.0.46",
                        "utf8mb4",
                        "utf8mb4_0900_ai_ci",
                        LocalDateTime.now(),
                        "1",
                        "Flyway migration V1 applied"));

        mockMvc.perform(get("/api/test/database")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-development-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.databaseName").value("novel_master"))
                .andExpect(jsonPath("$.flywayVersion").value("1"));
    }
}

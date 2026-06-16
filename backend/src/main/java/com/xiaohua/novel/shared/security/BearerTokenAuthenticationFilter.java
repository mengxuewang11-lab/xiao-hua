package com.xiaohua.novel.shared.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter
        implements InitializingBean {

    private static final String BEARER_PREFIX = "Bearer ";

    private final boolean authEnabled;
    private final byte[] expectedToken;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public BearerTokenAuthenticationFilter(
            @Value("${app.security.auth-enabled:false}") boolean authEnabled,
            @Value("${app.security.development-token:}") String developmentToken,
            RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.authEnabled = authEnabled;
        this.expectedToken = developmentToken.getBytes(StandardCharsets.UTF_8);
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.isTrue(
                !authEnabled || expectedToken.length > 0,
                "APP_AUTH_TOKEN must be configured when APP_AUTH_ENABLED=true");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!authEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith(BEARER_PREFIX)) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new InvalidBearerTokenAuthenticationException("Authorization 请求头格式错误"));
            return;
        }

        byte[] suppliedToken = authorization
                .substring(BEARER_PREFIX.length())
                .getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedToken, suppliedToken)) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new InvalidBearerTokenAuthenticationException("访问令牌无效"));
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "development-user",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_DEVELOPER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}

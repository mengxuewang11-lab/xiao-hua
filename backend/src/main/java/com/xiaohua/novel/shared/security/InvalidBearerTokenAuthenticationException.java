package com.xiaohua.novel.shared.security;

import org.springframework.security.core.AuthenticationException;

public class InvalidBearerTokenAuthenticationException extends AuthenticationException {

    public InvalidBearerTokenAuthenticationException(String message) {
        super(message);
    }
}

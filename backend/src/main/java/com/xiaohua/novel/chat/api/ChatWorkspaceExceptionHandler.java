package com.xiaohua.novel.chat.api;

import java.time.OffsetDateTime;

import com.xiaohua.novel.chat.application.ChatModelException;
import com.xiaohua.novel.chat.application.ChatWorkspaceNotFoundException;
import com.xiaohua.novel.shared.security.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(0)
@RestControllerAdvice(assignableTypes = ChatWorkspaceController.class)
public class ChatWorkspaceExceptionHandler {

    @ExceptionHandler(ChatWorkspaceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ChatWorkspaceNotFoundException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                "CHAT_CONVERSATION_NOT_FOUND",
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(ChatModelException.class)
    public ResponseEntity<ApiErrorResponse> handleModelException(
            ChatModelException exception,
            HttpServletRequest request) {
        HttpStatus status = "MODEL_HTTP_ERROR".equals(exception.code())
                || "MODEL_CALL_FAILED".equals(exception.code())
                || "MODEL_RESPONSE_EMPTY".equals(exception.code())
                ? HttpStatus.BAD_GATEWAY
                : HttpStatus.BAD_REQUEST;
        return response(status, exception.code(), exception.getMessage(), request);
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                status.value(),
                code,
                message,
                request.getRequestURI(),
                OffsetDateTime.now()));
    }
}

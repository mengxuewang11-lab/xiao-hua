package com.xiaohua.novel.agent.api;

import java.time.OffsetDateTime;

import com.xiaohua.novel.agent.application.NovelAgentConflictException;
import com.xiaohua.novel.agent.application.NovelAgentNotFoundException;
import com.xiaohua.novel.shared.security.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class NovelAgentExceptionHandler {

    @ExceptionHandler(NovelAgentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            NovelAgentNotFoundException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                "NOVEL_AGENT_NOT_FOUND",
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(NovelAgentConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            NovelAgentConflictException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.CONFLICT,
                "NOVEL_AGENT_STATE_CONFLICT",
                exception.getMessage(),
                request);
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        MethodArgumentNotValidException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            Exception exception,
            HttpServletRequest request) {
        String message = exception instanceof MethodArgumentNotValidException validation
                ? validation.getBindingResult().getFieldErrors().stream()
                        .findFirst()
                        .map(error -> error.getDefaultMessage())
                        .orElse("请求参数不合法")
                : exception.getMessage();
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                message,
                request);
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

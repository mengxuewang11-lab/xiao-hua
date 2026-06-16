package com.xiaohua.novel.chat.application;

public class ChatModelException extends RuntimeException {

    private final String code;

    public ChatModelException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ChatModelException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}

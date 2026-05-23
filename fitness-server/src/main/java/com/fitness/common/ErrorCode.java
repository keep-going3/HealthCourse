package com.fitness.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "不存在"),
    CONFLICT(409, "数据冲突"),
    TOO_MANY_REQUESTS(429, "服务繁忙"),
    SERVER_ERROR(500, "服务器错误"),
    SERVICE_UNAVAILABLE(503, "依赖不可用");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

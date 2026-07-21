package com.officeflow.common.api;

public enum ResultCode {
    SUCCESS(200, "success"),
    PARAM_ERROR(400, "param error"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    CONFLICT(409, "conflict"),
    FAIL(500, "fail"),
    SERVICE_UNAVAILABLE(503, "service unavailable");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}

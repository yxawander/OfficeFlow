package com.officeflow.common.api;

public enum ResultCode {
    SUCCESS(200, "success"),
    FAIL(500, "fail"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    PARAM_ERROR(400, "param error");

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


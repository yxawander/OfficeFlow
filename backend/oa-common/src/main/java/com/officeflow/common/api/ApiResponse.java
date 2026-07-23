package com.officeflow.common.api;

public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.code(), ResultCode.SUCCESS.message(), data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(ResultCode.SUCCESS.code(), ResultCode.SUCCESS.message(), null);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(ResultCode.FAIL.code(), message, null);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> fail(ResultCode resultCode) {
        return new ApiResponse<>(resultCode.code(), resultCode.message(), null);
    }
}

package com.officeflow.common.exception;

import com.officeflow.common.api.ResultCode;

public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.FAIL.code();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.message());
        this.code = resultCode.code();
    }

    public int getCode() {
        return code;
    }
}


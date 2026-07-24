package com.officeflow.common.exception;

import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.api.ResultCode;
import com.officeflow.common.ratelimit.RateLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        return ApiResponse.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentNotValidException.class
    })
    public ApiResponse<Void> handleParamException(Exception ex) {
        return ApiResponse.fail(ResultCode.PARAM_ERROR.code(), ex.getMessage());
    }

    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    @ExceptionHandler(RateLimitExceededException.class)
    public ApiResponse<Void> handleRateLimitException(RateLimitExceededException ex) {
        return ApiResponse.fail(ResultCode.TOO_MANY_REQUESTS.code(),
                ex.getMessage() + " (" + ex.getRetryAfterSeconds() + "s)");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        return ApiResponse.fail(ResultCode.FAIL.code(), ex.getMessage());
    }
}

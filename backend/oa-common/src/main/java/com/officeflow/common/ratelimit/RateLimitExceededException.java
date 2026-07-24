package com.officeflow.common.ratelimit;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a rate limit has been exceeded.
 * The @ResponseStatus annotation ensures Spring MVC sets HTTP 429 status code.
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitExceededException extends RuntimeException {

    private final int retryAfterSeconds;

    public RateLimitExceededException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

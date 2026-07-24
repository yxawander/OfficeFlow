package com.officeflow.common.ratelimit;

import java.lang.annotation.*;

/**
 * Rate limiting annotation for controller methods.
 * Uses Redis-backed fixed-window counter per user/IP.
 *
 * <p>Usage example:</p>
 * <pre>
 * {@code @RateLimit(key = "ai:query", maxRequests = 20, windowSeconds = 60)}
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Rate limit key identifier (e.g. "ai:query", "ai:upload").
     * Combined with user ID or IP to form the Redis key.
     */
    String key();

    /**
     * Maximum number of requests allowed within the time window.
     */
    int maxRequests();

    /**
     * Time window duration in seconds.
     */
    int windowSeconds();

    /**
     * Custom error message when rate limit is exceeded.
     */
    String message() default "请求过于频繁，请稍后再试";
}

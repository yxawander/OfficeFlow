package com.officeflow.common.ratelimit;

import com.officeflow.common.constant.CommonConstants;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * AOP aspect that intercepts methods annotated with {@link RateLimit}.
 * Uses Redis fixed-window counter keyed by endpoint + user/IP.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate stringRedisTemplate;

    @PostConstruct
    public void init() {
        log.info("RateLimitAspect initialized");
    }

    @Around("@annotation(com.officeflow.common.ratelimit.RateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        if (rateLimit == null) {
            return joinPoint.proceed();
        }

        String identifier = resolveIdentifier();
        String redisKey = CommonConstants.RATE_LIMIT_PREFIX + rateLimit.key() + ":" + identifier;

        Long count = stringRedisTemplate.opsForValue().increment(redisKey);

        // First request in this window: set the expiry
        if (count != null && count == 1) {
            stringRedisTemplate.expire(redisKey, rateLimit.windowSeconds(), TimeUnit.SECONDS);
        }

        if (count != null && count > rateLimit.maxRequests()) {
            Long ttl = stringRedisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            int retryAfter = ttl != null && ttl > 0 ? ttl.intValue() : rateLimit.windowSeconds();

            log.warn("Rate limit exceeded: key={}, count={}/{}, identifier={}",
                    rateLimit.key(), count, rateLimit.maxRequests(), identifier);

            throw new RateLimitExceededException(rateLimit.message(), retryAfter);
        }

        return joinPoint.proceed();
    }

    /**
     * Resolve user identity from gateway-injected headers, falling back to client IP.
     */
    private String resolveIdentifier() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();

                // Prefer user ID from gateway
                String userId = request.getHeader(CommonConstants.LOGIN_USER_ID_HEADER);
                if (userId != null && !userId.isBlank()) {
                    return "u:" + userId;
                }

                // Fallback to client IP (X-Forwarded-For aware)
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isBlank()) {
                    ip = request.getRemoteAddr();
                } else {
                    // Take the first IP in the chain
                    ip = ip.split(",")[0].trim();
                }
                return "ip:" + ip.replace(":", "_");
            }
        } catch (Exception e) {
            log.debug("Failed to resolve rate limit identifier: {}", e.getMessage());
        }
        return "anonymous";
    }
}

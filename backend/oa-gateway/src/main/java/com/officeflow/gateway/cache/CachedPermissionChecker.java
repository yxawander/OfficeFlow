package com.officeflow.gateway.cache;

import com.officeflow.common.constant.CommonConstants;
import com.officeflow.gateway.client.ApiPermissionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 基于 Redis 缓存的权限校验器 — 网关本地完成 AntPath 匹配，避免每次请求 HTTP 调用 user-service
 */
@Component
public class CachedPermissionChecker {

    private static final Logger log = LoggerFactory.getLogger(CachedPermissionChecker.class);
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final RedisTemplate<String, Object> redisTemplate;
    private final ApiPermissionClient apiPermissionClient;

    public CachedPermissionChecker(RedisTemplate<String, Object> redisTemplate,
                                   ApiPermissionClient apiPermissionClient) {
        this.redisTemplate = redisTemplate;
        this.apiPermissionClient = apiPermissionClient;
    }

    public Mono<Boolean> check(Long userId, String method, String path) {
        return Mono.fromCallable(() -> checkFromCache(userId, method, path))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(result -> {
                    if (result != null) {
                        return Mono.just(result);
                    }
                    log.debug("Cache miss for user {}, falling back to HTTP", userId);
                    return apiPermissionClient.check(userId, method, path)
                            .map(ApiPermissionClient.PermissionDecision::allowed);
                })
                .onErrorResume(ex -> {
                    log.warn("Permission check error for user {}, falling back to HTTP", userId, ex);
                    return apiPermissionClient.check(userId, method, path)
                            .map(ApiPermissionClient.PermissionDecision::allowed);
                });
    }

    @SuppressWarnings("unchecked")
    private Boolean checkFromCache(Long userId, String method, String path) {
        Object userPermObj = redisTemplate.opsForValue().get(CommonConstants.CACHE_USER_PERM_PREFIX + userId);
        if (!(userPermObj instanceof Map<?, ?> userPerm)) {
            return null;
        }
        if (Boolean.TRUE.equals(userPerm.get("admin"))) {
            return true;
        }

        Object apiRulesObj = redisTemplate.opsForValue().get(CommonConstants.CACHE_API_RULES);
        if (!(apiRulesObj instanceof List<?> apiRulesList)) {
            return null;
        }

        List<Map<String, Object>> rules = (List<Map<String, Object>>) (List<?>) apiRulesList;
        String safeMethod = method != null ? method.toUpperCase() : "";

        List<Map<String, Object>> matched = rules.stream()
                .filter(rule -> {
                    String ruleMethod = String.valueOf(rule.get("requestMethod"));
                    return "ALL".equals(ruleMethod) || safeMethod.equals(ruleMethod);
                })
                .filter(rule -> pathMatcher.match(String.valueOf(rule.get("requestPath")), path))
                .toList();

        if (matched.isEmpty()) {
            return true;
        }

        int maxLen = matched.stream()
                .map(r -> String.valueOf(r.get("requestPath")).length())
                .max(Comparator.naturalOrder())
                .orElse(0);
        List<Map<String, Object>> effective = matched.stream()
                .filter(r -> String.valueOf(r.get("requestPath")).length() == maxLen)
                .toList();

        Object permIdsObj = userPerm.get("apiPermIds");
        if (!(permIdsObj instanceof Collection<?> permIdsRaw)) {
            return false;
        }

        List<Long> permIds = permIdsRaw.stream()
                .filter(id -> id instanceof Number)
                .map(id -> ((Number) id).longValue())
                .toList();

        for (Map<String, Object> rule : effective) {
            Object ruleIdObj = rule.get("id");
            if (ruleIdObj instanceof Number ruleIdNum && permIds.contains(ruleIdNum.longValue())) {
                return true;
            }
        }

        return false;
    }
}

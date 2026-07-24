package com.officeflow.user.service;

import com.officeflow.common.constant.CommonConstants;
import com.officeflow.user.mapper.ApiPermissionMapper;
import com.officeflow.user.mapper.RoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户权限缓存服务 — 将用户权限上下文和全局 API 规则缓存到 Redis
 */
@Component
public class UserPermissionCacheService {

    private static final Logger log = LoggerFactory.getLogger(UserPermissionCacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final RoleMapper roleMapper;
    private final ApiPermissionMapper apiPermissionMapper;

    @Value("${officeflow.jwt.expire-seconds:86400}")
    private long jwtExpireSeconds;

    public UserPermissionCacheService(RedisTemplate<String, Object> redisTemplate,
                                      RoleMapper roleMapper,
                                      ApiPermissionMapper apiPermissionMapper) {
        this.redisTemplate = redisTemplate;
        this.roleMapper = roleMapper;
        this.apiPermissionMapper = apiPermissionMapper;
    }

    public void cacheUserPermissions(Long userId) {
        boolean isAdmin = roleMapper.listByUserId(userId).stream()
                .anyMatch(role -> "ADMIN".equals(String.valueOf(role.get("roleCode"))));
        Set<Long> apiPermIds = roleMapper.listApiPermissionIdsByUserId(userId).stream()
                .map(this::toLong)
                .collect(Collectors.toSet());
        Map<String, Object> context = new HashMap<>();
        context.put("admin", isAdmin);
        context.put("apiPermIds", apiPermIds);
        redisTemplate.opsForValue().set(CommonConstants.CACHE_USER_PERM_PREFIX + userId, context, Duration.ofSeconds(jwtExpireSeconds));
        log.debug("Cached permissions for user {}", userId);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getCachedUserPermissions(Long userId) {
        Object value = redisTemplate.opsForValue().get(CommonConstants.CACHE_USER_PERM_PREFIX + userId);
        return (value instanceof Map<?, ?> map) ? (Map<String, Object>) map : null;
    }

    public void evictUserPermissions(Long userId) {
        redisTemplate.delete(CommonConstants.CACHE_USER_PERM_PREFIX + userId);
    }

    public void evictAllPermissionCache() {
        Set<String> keys = redisTemplate.keys(CommonConstants.CACHE_USER_PERM_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        redisTemplate.delete(CommonConstants.CACHE_API_RULES);
    }

    public void cacheApiRules() {
        List<Map<String, Object>> rules = apiPermissionMapper.listAllEnabled();
        redisTemplate.opsForValue().set(CommonConstants.CACHE_API_RULES, rules, Duration.ofMinutes(30));
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getCachedApiRules() {
        Object value = redisTemplate.opsForValue().get(CommonConstants.CACHE_API_RULES);
        return (value instanceof List<?> list) ? (List<Map<String, Object>>) list : null;
    }

    public List<Map<String, Object>> getApiRules() {
        List<Map<String, Object>> rules = getCachedApiRules();
        if (rules == null) {
            rules = apiPermissionMapper.listAllEnabled();
            redisTemplate.opsForValue().set(CommonConstants.CACHE_API_RULES, rules, Duration.ofMinutes(30));
        }
        return rules;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        return Long.valueOf(String.valueOf(value));
    }
}

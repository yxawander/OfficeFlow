package com.officeflow.gateway.cache;

import com.officeflow.common.constant.CommonConstants;
import com.officeflow.gateway.client.ApiPermissionClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("网关权限缓存测试")
class CachedPermissionCheckerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private ApiPermissionClient apiPermissionClient;

    @Test
    @DisplayName("缓存未命中时必须回源，不能完成为空流")
    void cacheMissShouldFallBackToPermissionService() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CommonConstants.CACHE_USER_PERM_PREFIX + 1L)).thenReturn(null);
        when(apiPermissionClient.check(1L, "GET", "/api/user/roles"))
                .thenReturn(Mono.just(new ApiPermissionClient.PermissionDecision(true, true, "管理员放行")));

        Boolean allowed = new CachedPermissionChecker(redisTemplate, apiPermissionClient)
                .check(1L, "GET", "/api/user/roles")
                .block();

        assertThat(allowed).isTrue();
        verify(apiPermissionClient).check(1L, "GET", "/api/user/roles");
    }

    @Test
    @DisplayName("管理员缓存命中时直接放行")
    void cachedAdminShouldBeAllowedWithoutRemoteCall() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CommonConstants.CACHE_USER_PERM_PREFIX + 1L))
                .thenReturn(java.util.Map.of("admin", true));

        Boolean allowed = new CachedPermissionChecker(redisTemplate, apiPermissionClient)
                .check(1L, "GET", "/api/user/roles")
                .block();

        assertThat(allowed).isTrue();
        verify(apiPermissionClient, never()).check(1L, "GET", "/api/user/roles");
    }
}

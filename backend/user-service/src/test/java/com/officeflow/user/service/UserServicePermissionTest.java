package com.officeflow.user.service;

import com.officeflow.user.mapper.ApiPermissionMapper;
import com.officeflow.user.mapper.DeptMapper;
import com.officeflow.user.mapper.LogMapper;
import com.officeflow.user.mapper.MenuMapper;
import com.officeflow.user.mapper.PostMapper;
import com.officeflow.user.mapper.RoleMapper;
import com.officeflow.user.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户权限缓存恢复测试")
class UserServicePermissionTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private DeptMapper deptMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private MenuMapper menuMapper;
    @Mock
    private ApiPermissionMapper apiPermissionMapper;
    @Mock
    private LogMapper logMapper;
    @Mock
    private UserPermissionCacheService permissionCacheService;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("管理员权限缓存被清理后应从数据库重建并继续放行")
    void adminShouldBeAllowedAfterPermissionCacheEviction() {
        when(permissionCacheService.getOrLoadUserPermissions(1L))
                .thenReturn(Map.of("admin", true, "apiPermIds", Set.of()));

        Map<String, Object> result =
                userService.checkApiPermission(1L, "GET", "/api/user/roles");

        assertThat(result.get("allowed")).isEqualTo(true);
        assertThat(result.get("permissionCode")).isEqualTo("ADMIN");
        verify(permissionCacheService).getOrLoadUserPermissions(1L);
        verifyNoInteractions(apiPermissionMapper);
    }

    @Test
    @DisplayName("普通用户权限缓存被清理后应使用重建的接口权限")
    void userPermissionsShouldBeUsedAfterPermissionCacheEviction() {
        when(permissionCacheService.getOrLoadUserPermissions(2L))
                .thenReturn(Map.of("admin", false, "apiPermIds", Set.of(2L)));
        when(permissionCacheService.getApiRules()).thenReturn(List.of(Map.of(
                "id", 2L,
                "requestMethod", "GET",
                "requestPath", "/api/user/**",
                "permissionCode", "api:user:list"
        )));

        Map<String, Object> result =
                userService.checkApiPermission(2L, "GET", "/api/user/roles");

        assertThat(result.get("allowed")).isEqualTo(true);
        assertThat(result.get("permissionCode")).isEqualTo("api:user:list");
    }
}

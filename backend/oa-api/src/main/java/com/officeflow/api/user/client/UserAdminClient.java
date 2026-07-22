package com.officeflow.api.user.client;

import com.officeflow.api.user.dto.*;
import com.officeflow.api.user.vo.*;
import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.api.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "user-service", path = "/api/user")
public interface UserAdminClient {

    // ==================== User Management ====================

    @GetMapping("/users")
    ApiResponse<PageResult<UserVO>> getUserPage(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Long deptId,
                                                 @RequestParam(required = false) Integer status,
                                                 @RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize);

    @GetMapping("/users/options")
    ApiResponse<List<UserOptionVO>> getUserOptions();

    @PostMapping("/users")
    ApiResponse<Void> createUser(@RequestBody UserRequest request,
                                  @RequestHeader("X-Login-User-Id") Long userId,
                                  @RequestHeader("X-Login-Username") String username);

    @PutMapping("/users/{id}")
    ApiResponse<Void> updateUser(@PathVariable Long id,
                                  @RequestBody UserRequest request,
                                  @RequestHeader("X-Login-User-Id") Long userId,
                                  @RequestHeader("X-Login-Username") String username);

    @DeleteMapping("/users/{id}")
    ApiResponse<Void> deleteUser(@PathVariable Long id,
                                  @RequestHeader("X-Login-User-Id") Long userId,
                                  @RequestHeader("X-Login-Username") String username);

    @PutMapping("/users/{id}/status")
    ApiResponse<Void> updateUserStatus(@PathVariable Long id,
                                        @RequestBody StatusRequest request,
                                        @RequestHeader("X-Login-User-Id") Long userId,
                                        @RequestHeader("X-Login-Username") String username);

    @PutMapping("/users/{id}/password")
    ApiResponse<Void> resetPassword(@PathVariable Long id,
                                     @RequestBody PasswordResetRequest request,
                                     @RequestHeader("X-Login-User-Id") Long userId,
                                     @RequestHeader("X-Login-Username") String username);

    @PutMapping("/users/{id}/roles")
    ApiResponse<Void> assignUserRoles(@PathVariable Long id,
                                       @RequestBody IdListRequest request,
                                       @RequestHeader("X-Login-User-Id") Long userId,
                                       @RequestHeader("X-Login-Username") String username);

    // ==================== Department Management ====================

    @GetMapping("/depts")
    ApiResponse<List<DeptVO>> getDeptList();

    @GetMapping("/depts/tree")
    ApiResponse<List<Object>> getDeptTree();

    @PostMapping("/depts")
    ApiResponse<Void> createDept(@RequestBody DeptRequest request,
                                  @RequestHeader("X-Login-User-Id") Long userId,
                                  @RequestHeader("X-Login-Username") String username);

    @PutMapping("/depts/{id}")
    ApiResponse<Void> updateDept(@PathVariable Long id,
                                  @RequestBody DeptRequest request,
                                  @RequestHeader("X-Login-User-Id") Long userId,
                                  @RequestHeader("X-Login-Username") String username);

    @DeleteMapping("/depts/{id}")
    ApiResponse<Void> deleteDept(@PathVariable Long id,
                                  @RequestHeader("X-Login-User-Id") Long userId,
                                  @RequestHeader("X-Login-Username") String username);

    // ==================== Post Management ====================

    @GetMapping("/posts")
    ApiResponse<List<PostVO>> getPostList();

    @PostMapping("/posts")
    ApiResponse<Void> createPost(@RequestBody PostRequest request,
                                  @RequestHeader("X-Login-User-Id") Long userId,
                                  @RequestHeader("X-Login-Username") String username);

    @PutMapping("/posts/{id}")
    ApiResponse<Void> updatePost(@PathVariable Long id,
                                  @RequestBody PostRequest request,
                                  @RequestHeader("X-Login-User-Id") Long userId,
                                  @RequestHeader("X-Login-Username") String username);

    @DeleteMapping("/posts/{id}")
    ApiResponse<Void> deletePost(@PathVariable Long id,
                                  @RequestHeader("X-Login-User-Id") Long userId,
                                  @RequestHeader("X-Login-Username") String username);

    // ==================== Role Management ====================

    @GetMapping("/roles")
    ApiResponse<List<RoleVO>> getRoleList();

    @PostMapping("/roles")
    ApiResponse<Void> createRole(@RequestBody RoleRequest request,
                                  @RequestHeader("X-Login-User-Id") Long userId,
                                  @RequestHeader("X-Login-Username") String username);

    @PutMapping("/roles/{id}")
    ApiResponse<Void> updateRole(@PathVariable Long id,
                                  @RequestBody RoleRequest request,
                                  @RequestHeader("X-Login-User-Id") Long userId,
                                  @RequestHeader("X-Login-Username") String username);

    @DeleteMapping("/roles/{id}")
    ApiResponse<Void> deleteRole(@PathVariable Long id,
                                  @RequestHeader("X-Login-User-Id") Long userId,
                                  @RequestHeader("X-Login-Username") String username);

    @PutMapping("/roles/{id}/menus")
    ApiResponse<Void> assignRoleMenus(@PathVariable Long id,
                                       @RequestBody IdListRequest request,
                                       @RequestHeader("X-Login-User-Id") Long userId,
                                       @RequestHeader("X-Login-Username") String username);

    @PutMapping("/roles/{id}/api-permissions")
    ApiResponse<Void> assignRoleApiPermissions(@PathVariable Long id,
                                                @RequestBody IdListRequest request,
                                                @RequestHeader("X-Login-User-Id") Long userId,
                                                @RequestHeader("X-Login-Username") String username);

    // ==================== Permission Management ====================

    @GetMapping("/menus")
    ApiResponse<List<MenuVO>> getMenuList();

    @GetMapping("/api-permissions")
    ApiResponse<List<ApiPermissionVO>> getApiPermissionList();

    @PostMapping("/api-permissions")
    ApiResponse<Void> createApiPermission(@RequestBody ApiPermissionRequest request,
                                           @RequestHeader("X-Login-User-Id") Long userId,
                                           @RequestHeader("X-Login-Username") String username);

    @PutMapping("/api-permissions/{id}")
    ApiResponse<Void> updateApiPermission(@PathVariable Long id,
                                           @RequestBody ApiPermissionRequest request,
                                           @RequestHeader("X-Login-User-Id") Long userId,
                                           @RequestHeader("X-Login-Username") String username);

    @DeleteMapping("/api-permissions/{id}")
    ApiResponse<Void> disableApiPermission(@PathVariable Long id,
                                            @RequestHeader("X-Login-User-Id") Long userId,
                                            @RequestHeader("X-Login-Username") String username);

    // ==================== Logs ====================

    @GetMapping("/logs/login")
    ApiResponse<List<LoginLogVO>> getLoginLogs();

    @GetMapping("/logs/operations")
    ApiResponse<List<OperationLogVO>> getOperationLogs();
}

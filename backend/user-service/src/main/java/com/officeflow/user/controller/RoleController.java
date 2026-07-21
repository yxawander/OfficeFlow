package com.officeflow.user.controller;

import com.officeflow.common.api.ApiResponse;
import com.officeflow.user.dto.IdListRequest;
import com.officeflow.user.dto.RoleRequest;
import com.officeflow.user.service.UserService;
import com.officeflow.user.util.RequestUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/roles")
public class RoleController {
    private final UserService userService;

    public RoleController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<Object> list() {
        return ApiResponse.ok(userService.roleList());
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody RoleRequest request, HttpServletRequest httpRequest) {
        userService.createRole(request);
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "角色管理", "CREATE", httpRequest);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody RoleRequest request, HttpServletRequest httpRequest) {
        userService.updateRole(id, request);
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "角色管理", "UPDATE", httpRequest);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        userService.deleteRole(id);
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "角色管理", "DELETE", httpRequest);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/menus")
    public ApiResponse<Void> assignMenus(@PathVariable Long id, @Valid @RequestBody IdListRequest request, HttpServletRequest httpRequest) {
        userService.assignRoleMenus(id, request.ids());
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "角色菜单", "ASSIGN", httpRequest);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/api-permissions")
    public ApiResponse<Void> assignApiPermissions(@PathVariable Long id, @Valid @RequestBody IdListRequest request, HttpServletRequest httpRequest) {
        userService.assignRoleApiPermissions(id, request.ids());
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "角色接口权限", "ASSIGN", httpRequest);
        return ApiResponse.ok();
    }
}

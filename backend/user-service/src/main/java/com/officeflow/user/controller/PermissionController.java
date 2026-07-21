package com.officeflow.user.controller;

import com.officeflow.common.api.ApiResponse;
import com.officeflow.user.dto.ApiPermissionRequest;
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
@RequestMapping("/api/user")
public class PermissionController {
    private final UserService userService;

    public PermissionController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/menus")
    public ApiResponse<Object> menus() {
        return ApiResponse.ok(userService.allMenus());
    }

    @GetMapping("/api-permissions")
    public ApiResponse<Object> apiPermissions() {
        return ApiResponse.ok(userService.apiPermissionList());
    }

    @PostMapping("/api-permissions")
    public ApiResponse<Void> createApiPermission(@Valid @RequestBody ApiPermissionRequest request, HttpServletRequest httpRequest) {
        userService.createApiPermission(request);
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "接口权限", "CREATE", httpRequest);
        return ApiResponse.ok();
    }

    @PutMapping("/api-permissions/{id}")
    public ApiResponse<Void> updateApiPermission(@PathVariable Long id, @Valid @RequestBody ApiPermissionRequest request, HttpServletRequest httpRequest) {
        userService.updateApiPermission(id, request);
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "接口权限", "UPDATE", httpRequest);
        return ApiResponse.ok();
    }

    @DeleteMapping("/api-permissions/{id}")
    public ApiResponse<Void> disableApiPermission(@PathVariable Long id, HttpServletRequest httpRequest) {
        userService.disableApiPermission(id);
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "接口权限", "DISABLE", httpRequest);
        return ApiResponse.ok();
    }

    @GetMapping("/logs/login")
    public ApiResponse<Object> loginLogs() {
        return ApiResponse.ok(userService.loginLogs(100));
    }

    @GetMapping("/logs/operations")
    public ApiResponse<Object> operationLogs() {
        return ApiResponse.ok(userService.operationLogs(100));
    }
}

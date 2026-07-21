package com.officeflow.user.controller;

import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.api.PageResult;
import com.officeflow.user.dto.IdListRequest;
import com.officeflow.user.dto.PasswordResetRequest;
import com.officeflow.user.dto.StatusRequest;
import com.officeflow.user.dto.UserRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> page(@RequestParam(name = "keyword", required = false) String keyword,
                                                             @RequestParam(name = "deptId", required = false) Long deptId,
                                                             @RequestParam(name = "status", required = false) Integer status,
                                                             @RequestParam(name = "pageNum", defaultValue = "1") long pageNum,
                                                             @RequestParam(name = "pageSize", defaultValue = "10") long pageSize) {
        return ApiResponse.ok(userService.userPage(keyword, deptId, status, pageNum, pageSize));
    }

    @GetMapping("/options")
    public ApiResponse<Object> options() {
        return ApiResponse.ok(userService.enabledUsers());
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody UserRequest request, HttpServletRequest httpRequest) {
        userService.createUser(request);
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "员工管理", "CREATE", httpRequest);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody UserRequest request, HttpServletRequest httpRequest) {
        userService.updateUser(id, request);
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "员工管理", "UPDATE", httpRequest);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        userService.deleteUser(id);
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "员工管理", "DELETE", httpRequest);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> status(@PathVariable Long id, @Valid @RequestBody StatusRequest request, HttpServletRequest httpRequest) {
        userService.updateUserStatus(id, request.status());
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "员工管理", "UPDATE_STATUS", httpRequest);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request, HttpServletRequest httpRequest) {
        userService.resetPassword(id, request.password());
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "员工管理", "RESET_PASSWORD", httpRequest);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/roles")
    public ApiResponse<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody IdListRequest request, HttpServletRequest httpRequest) {
        userService.assignUserRoles(id, request.ids());
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "员工角色", "ASSIGN", httpRequest);
        return ApiResponse.ok();
    }
}

package com.officeflow.user.controller;

import com.officeflow.common.api.ApiResponse;
import com.officeflow.user.dto.DeptRequest;
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
@RequestMapping("/api/user/depts")
public class DeptController {
    private final UserService userService;

    public DeptController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/tree")
    public ApiResponse<Object> tree() {
        return ApiResponse.ok(userService.deptTree());
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody DeptRequest request, HttpServletRequest httpRequest) {
        userService.createDept(request);
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "部门管理", "CREATE", httpRequest);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody DeptRequest request, HttpServletRequest httpRequest) {
        userService.updateDept(id, request);
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "部门管理", "UPDATE", httpRequest);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        userService.deleteDept(id);
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "部门管理", "DELETE", httpRequest);
        return ApiResponse.ok();
    }
}

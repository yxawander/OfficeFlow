package com.officeflow.user.controller;

import com.officeflow.common.api.ApiResponse;
import com.officeflow.user.dto.LoginRequest;
import com.officeflow.user.service.UserService;
import com.officeflow.user.util.RequestUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping({"/login", "/auth/login"})
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(userService.login(request.username(), request.password(), httpRequest));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        userService.operationLog(RequestUser.userId(request), RequestUser.username(request), "用户认证", "LOGOUT", request);
        return ApiResponse.ok();
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile(HttpServletRequest request) {
        return ApiResponse.ok(userService.profile(RequestUser.userId(request)));
    }

    @GetMapping("/menus/current")
    public ApiResponse<Object> currentMenus(HttpServletRequest request) {
        return ApiResponse.ok(userService.currentMenus(RequestUser.userId(request)));
    }
}

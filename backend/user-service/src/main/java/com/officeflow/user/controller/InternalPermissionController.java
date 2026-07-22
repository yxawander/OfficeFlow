package com.officeflow.user.controller;

import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.constant.CommonConstants;
import com.officeflow.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user/internal/permissions")
public class InternalPermissionController {
    private final UserService userService;

    @Value("${officeflow.internal.secret:officeflow-internal-secret}")
    private String internalSecret;

    public InternalPermissionController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/check")
    public ApiResponse<Map<String, Object>> check(@RequestHeader(name = CommonConstants.INTERNAL_TOKEN_HEADER, required = false) String token,
                                                  @RequestParam Long userId,
                                                  @RequestParam String method,
                                                  @RequestParam String path) {
        if (!internalSecret.equals(token)) {
            return new ApiResponse<>(403, "非法内部调用", null);
        }
        return ApiResponse.ok(userService.checkApiPermission(userId, method, path));
    }
}

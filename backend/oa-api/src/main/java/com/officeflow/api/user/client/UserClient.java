package com.officeflow.api.user.client;

import com.officeflow.api.user.dto.LoginRequest;
import com.officeflow.api.user.vo.LoginResultVO;
import com.officeflow.api.user.vo.UserProfileVO;
import com.officeflow.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@FeignClient(name = "user-service", path = "/api/user")
public interface UserClient {

    @GetMapping("/health")
    ApiResponse<String> health();

    @PostMapping({"/login", "/auth/login"})
    ApiResponse<LoginResultVO> login(@RequestBody LoginRequest request);

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestHeader("X-Login-User-Id") Long userId,
                             @RequestHeader("X-Login-Username") String username);

    @GetMapping("/profile")
    ApiResponse<UserProfileVO> profile(@RequestHeader("X-Login-User-Id") Long userId);

    @GetMapping("/menus/current")
    ApiResponse<List<Object>> currentMenus(@RequestHeader("X-Login-User-Id") Long userId);
}

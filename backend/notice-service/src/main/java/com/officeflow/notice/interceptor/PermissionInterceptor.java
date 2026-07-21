package com.officeflow.notice.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.constant.CommonConstants;
import com.officeflow.common.api.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private static final List<String> ADMIN_ROLES = Arrays.asList("ADMIN", "MANAGER");
    private static final String ADMIN_PATH_PREFIX = "/api/notice/admin";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        if (path.startsWith(ADMIN_PATH_PREFIX)) {
            return checkAdminPermission(request, response);
        }

        return true;
    }

    private boolean checkAdminPermission(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String rolesStr = request.getHeader(CommonConstants.LOGIN_ROLES_HEADER);

        if (rolesStr == null || rolesStr.isEmpty()) {
            log.warn("缺少角色信息，拒绝访问管理员接口: {}", request.getRequestURI());
            sendErrorResponse(response, ResultCode.FORBIDDEN, "无权访问");
            return false;
        }

        List<String> roles = Arrays.asList(rolesStr.split(","));

        boolean hasAdminRole = ADMIN_ROLES.stream().anyMatch(roles::contains);

        if (!hasAdminRole) {
            log.warn("用户角色 {} 无权访问管理员接口: {}", roles, request.getRequestURI());
            sendErrorResponse(response, ResultCode.FORBIDDEN, "无权访问");
            return false;
        }

        return true;
    }

    private void sendErrorResponse(HttpServletResponse response, ResultCode resultCode, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        ApiResponse<?> apiResponse = ApiResponse.fail(resultCode.code(), message);

        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(apiResponse));
        response.getWriter().flush();
    }
}
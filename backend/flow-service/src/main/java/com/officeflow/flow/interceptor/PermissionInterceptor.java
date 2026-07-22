package com.officeflow.flow.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.api.ResultCode;
import com.officeflow.common.constant.CommonConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private static final List<String> ADMIN_ROLES = Arrays.asList("ADMIN", "MANAGER");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String rolesStr = request.getHeader(CommonConstants.LOGIN_ROLES_HEADER);
        if (rolesStr == null || rolesStr.isEmpty()) {
            writeForbidden(response, "缺少角色信息");
            return false;
        }

        String cleaned = rolesStr.replace("[", "").replace("]", "").trim();
        List<String> roles = Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .toList();
        boolean hasPermission = roles.stream().anyMatch(ADMIN_ROLES::contains);

        if (!hasPermission) {
            writeForbidden(response, "权限不足，仅管理员和经理可操作");
            return false;
        }

        return true;
    }

    private void writeForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<Void> apiResponse = ApiResponse.fail(ResultCode.FORBIDDEN.code(), message);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}

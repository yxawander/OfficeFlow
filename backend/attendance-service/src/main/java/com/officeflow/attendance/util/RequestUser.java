package com.officeflow.attendance.util;

import com.officeflow.common.constant.CommonConstants;
import jakarta.servlet.http.HttpServletRequest;

public final class RequestUser {
    private RequestUser() {
    }

    public static Long userId(HttpServletRequest request) {
        String value = request.getHeader(CommonConstants.LOGIN_USER_ID_HEADER);
        if (value == null || value.isBlank() || "null".equals(value)) {
            return null;
        }
        return Long.valueOf(value);
    }

    public static String username(HttpServletRequest request) {
        String value = request.getHeader(CommonConstants.LOGIN_USERNAME_HEADER);
        return value == null || "null".equals(value) ? null : value;
    }

    public static String roles(HttpServletRequest request) {
        String value = request.getHeader(CommonConstants.LOGIN_ROLES_HEADER);
        return value == null || "null".equals(value) ? "" : value;
    }
}

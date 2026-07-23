package com.officeflow.common.constant;

public final class CommonConstants {
    private CommonConstants() {
    }

    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;
    public static final int DELETED_NO = 0;
    public static final int DELETED_YES = 1;
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String LOGIN_USER_HEADER = "X-Login-User";
    public static final String LOGIN_USER_ID_HEADER = "X-Login-User-Id";
    public static final String LOGIN_USERNAME_HEADER = "X-Login-Username";
    public static final String LOGIN_ROLES_HEADER = "X-Login-Roles";
    public static final String LOGIN_DEPT_ID_HEADER = "X-Login-DeptId";
    public static final String INTERNAL_TOKEN_HEADER = "X-OfficeFlow-Internal-Token";
}


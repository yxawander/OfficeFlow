package com.officeflow.common.security;

public final class LoginUserHolder {
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private LoginUserHolder() {
    }

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser loginUser = HOLDER.get();
        return loginUser == null ? null : loginUser.userId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}


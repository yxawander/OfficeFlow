package com.officeflow.common.security;

import java.util.List;

public record LoginUser(
        Long userId,
        String username,
        String realName,
        Long deptId,
        List<String> roles,
        List<String> permissions
) {
}


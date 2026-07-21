package com.officeflow.user.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleRequest(
        @NotBlank(message = "角色名称不能为空") String roleName,
        @NotBlank(message = "角色编码不能为空") String roleCode,
        String dataScope,
        Integer sortOrder,
        Integer status
) {
}

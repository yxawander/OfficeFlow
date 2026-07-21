package com.officeflow.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ApiPermissionRequest(
        @NotBlank(message = "权限名称不能为空") String permissionName,
        @NotBlank(message = "权限编码不能为空") String permissionCode,
        @NotBlank(message = "服务名称不能为空") String serviceName,
        @NotBlank(message = "请求方法不能为空") String requestMethod,
        @NotBlank(message = "请求路径不能为空") String requestPath,
        Integer status
) {
}

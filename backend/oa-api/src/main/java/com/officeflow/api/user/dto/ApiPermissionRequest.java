package com.officeflow.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApiPermissionRequest {
    @NotBlank(message = "权限名称不能为空")
    private String permissionName;

    @NotBlank(message = "权限编码不能为空")
    private String permissionCode;

    @NotBlank(message = "服务名称不能为空")
    private String serviceName;

    @NotBlank(message = "请求方法不能为空")
    private String requestMethod;

    @NotBlank(message = "请求路径不能为空")
    private String requestPath;

    private Integer status;
}

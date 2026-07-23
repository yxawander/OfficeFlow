package com.officeflow.api.user.vo;

import lombok.Data;

@Data
public class ApiPermissionVO {
    private Long id;
    private String permissionName;
    private String permissionCode;
    private String serviceName;
    private String requestMethod;
    private String requestPath;
    private Integer status;
}

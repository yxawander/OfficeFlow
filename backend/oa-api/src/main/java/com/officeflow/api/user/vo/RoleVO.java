package com.officeflow.api.user.vo;

import lombok.Data;

@Data
public class RoleVO {
    private Long id;
    private String roleName;
    private String roleCode;
    private String dataScope;
    private Integer sortOrder;
    private Integer status;
}

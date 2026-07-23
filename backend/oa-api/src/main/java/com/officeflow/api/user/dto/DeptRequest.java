package com.officeflow.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeptRequest {
    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    @NotBlank(message = "部门编码不能为空")
    private String deptCode;

    private Long leaderId;
    private String phone;
    private String email;
    private Integer sortOrder;
    private Integer status;
}

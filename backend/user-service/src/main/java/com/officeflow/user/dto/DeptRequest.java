package com.officeflow.user.dto;

import jakarta.validation.constraints.NotBlank;

public record DeptRequest(
        Long parentId,
        @NotBlank(message = "部门名称不能为空") String deptName,
        @NotBlank(message = "部门编码不能为空") String deptCode,
        Long leaderId,
        String phone,
        String email,
        Integer sortOrder,
        Integer status
) {
}

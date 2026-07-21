package com.officeflow.user.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record UserRequest(
        @NotBlank(message = "账号不能为空") String username,
        String password,
        @NotBlank(message = "姓名不能为空") String realName,
        Integer gender,
        String phone,
        String email,
        String avatar,
        Long deptId,
        Long postId,
        Long managerId,
        LocalDate hireDate,
        String userType,
        Integer status
) {
}

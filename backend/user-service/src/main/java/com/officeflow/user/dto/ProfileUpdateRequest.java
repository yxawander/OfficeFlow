package com.officeflow.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ProfileUpdateRequest(
        @NotBlank(message = "姓名不能为空") String realName,
        Integer gender,
        String phone,
        String email,
        String avatar
) {
}

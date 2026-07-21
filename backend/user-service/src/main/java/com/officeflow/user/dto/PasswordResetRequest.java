package com.officeflow.user.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(@NotBlank(message = "密码不能为空") String password) {
}

package com.officeflow.user.dto;

import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull(message = "状态不能为空") Integer status) {
}

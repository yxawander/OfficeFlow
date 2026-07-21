package com.officeflow.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record IdListRequest(@NotNull(message = "ID列表不能为空") List<Long> ids) {
}

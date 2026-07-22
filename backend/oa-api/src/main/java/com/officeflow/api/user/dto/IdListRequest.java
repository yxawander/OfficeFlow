package com.officeflow.api.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class IdListRequest {
    @NotNull(message = "ID列表不能为空")
    private List<Long> ids;
}

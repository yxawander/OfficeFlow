package com.officeflow.user.dto;

import jakarta.validation.constraints.NotBlank;

public record PostRequest(
        @NotBlank(message = "岗位名称不能为空") String postName,
        @NotBlank(message = "岗位编码不能为空") String postCode,
        Integer sortOrder,
        Integer status
) {
}

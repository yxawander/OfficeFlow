package com.officeflow.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PostRequest {
    @NotBlank(message = "岗位名称不能为空")
    private String postName;

    @NotBlank(message = "岗位编码不能为空")
    private String postCode;

    private Integer sortOrder;
    private Integer status;
}

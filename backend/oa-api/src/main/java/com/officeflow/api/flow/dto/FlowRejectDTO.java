package com.officeflow.api.flow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FlowRejectDTO {
    @NotBlank(message = "驳回意见不能为空")
    private String comment;
}

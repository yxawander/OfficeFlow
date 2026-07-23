package com.officeflow.api.flow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FlowApproveDTO {
    private String comment;
}

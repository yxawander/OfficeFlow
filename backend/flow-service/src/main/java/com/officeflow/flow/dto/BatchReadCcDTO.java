package com.officeflow.flow.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchReadCcDTO {
    @NotEmpty(message = "抄送ID列表不能为空")
    private List<Long> ccIds;
}

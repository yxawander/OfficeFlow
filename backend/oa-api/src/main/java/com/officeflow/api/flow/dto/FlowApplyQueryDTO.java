package com.officeflow.api.flow.dto;

import lombok.Data;

@Data
public class FlowApplyQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private Integer offset;
    private String applyType;
    private String status;
    private String startDate;
    private String endDate;
}

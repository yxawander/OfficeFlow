package com.officeflow.api.flow.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FlowApproveRecordVO {
    private Long id;
    private Long approverId;
    private String approverName;
    private String action;
    private String comment;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvedAt;
}

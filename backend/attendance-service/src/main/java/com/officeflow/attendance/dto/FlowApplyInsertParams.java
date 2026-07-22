package com.officeflow.attendance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FlowApplyInsertParams {
    private Long id;
    private String applyNo;
    private Long applicantId;
    private Long deptId;
    private Long approverId;
    private String applyType;
    private String title;
    private String reason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal durationHours;
    private String status;
    private String currentNode;
}

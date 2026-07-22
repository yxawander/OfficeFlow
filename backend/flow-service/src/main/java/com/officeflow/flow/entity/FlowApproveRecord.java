package com.officeflow.flow.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FlowApproveRecord {
    private Long id;
    private Long flowApplyId;
    private Long approverId;
    private String action;
    private String comment;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

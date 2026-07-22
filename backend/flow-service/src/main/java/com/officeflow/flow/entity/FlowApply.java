package com.officeflow.flow.entity;

import com.officeflow.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class FlowApply extends BaseEntity {
    private Long id;
    private String applyNo;
    private Long applicantId;
    private Long applicantDeptId;
    private Long approverId;
    private String applyType;
    private String title;
    private String reason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal durationHours;
    private String status;
    private String currentNode;
    private LocalDateTime approvedAt;
    private Byte isDeleted;
}

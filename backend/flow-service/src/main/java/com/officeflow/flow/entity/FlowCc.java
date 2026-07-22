package com.officeflow.flow.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FlowCc {
    private Long id;
    private Long flowApplyId;
    private Long userId;
    private Byte readStatus;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

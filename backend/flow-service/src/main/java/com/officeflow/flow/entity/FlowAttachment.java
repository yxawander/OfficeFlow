package com.officeflow.flow.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FlowAttachment {
    private Long id;
    private Long flowApplyId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private Long uploadedBy;
    private LocalDateTime createdAt;
}

package com.officeflow.flow.vo;

import lombok.Data;

@Data
public class AttachmentVO {
    private Long id;
    private Long flowApplyId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
}

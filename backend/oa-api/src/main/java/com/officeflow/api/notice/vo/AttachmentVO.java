package com.officeflow.api.notice.vo;

import lombok.Data;

@Data
public class AttachmentVO {
    private Long id;
    private Long noticeId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
}

package com.officeflow.notice.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeAttachment {
    private Long id;
    private Long noticeId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private String ossKey;
    private Long uploadedBy;
    private LocalDateTime createdAt;
}

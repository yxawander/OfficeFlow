package com.officeflow.notice.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeRead {
    private Long id;
    private Long noticeId;
    private Long userId;
    private Byte readStatus;
    private LocalDateTime readAt;
    private String readIp;
    private Integer readDurationSeconds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
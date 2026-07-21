package com.officeflow.notice.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeScope {
    private Long id;
    private Long noticeId;
    private String scopeType;
    private Long scopeId;
    private LocalDateTime createdAt;
}
package com.officeflow.notice.entity;

import com.officeflow.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class Notice extends BaseEntity {
    private Long id;
    private String title;
    private String content;
    private String noticeType;
    private String priority;
    private Long publisherId;
    private String publisherName;
    private LocalDateTime publishTime;
    private LocalDateTime scheduledTime;
    private LocalDateTime expireTime;
    private String status;
    private Integer readCount;
    private Integer viewCount;
    private Byte isDeleted;
}
package com.officeflow.notice.dto;

import lombok.Data;

@Data
public class NoticeQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private Integer offset;
    private String keyword;
    private String noticeType;
    private String priority;
    private Integer readStatus;
    private Boolean onlyPublished = true;
    private String status;
    private Long publisherId;
    private String startDate;
    private String endDate;
}
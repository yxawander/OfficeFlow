package com.officeflow.api.notice.vo;

import lombok.Data;

import java.util.Map;

@Data
public class UnreadCountVO {
    private Long total;
    private Map<String, Long> byType;
    private Map<String, Long> byPriority;
}
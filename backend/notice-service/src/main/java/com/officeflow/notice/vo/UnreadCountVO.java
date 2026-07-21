package com.officeflow.notice.vo;

import lombok.Data;

import java.util.Map;

@Data
public class UnreadCountVO {
    private Integer total;
    private Map<String, Integer> byType;
    private Map<String, Integer> byPriority;
}
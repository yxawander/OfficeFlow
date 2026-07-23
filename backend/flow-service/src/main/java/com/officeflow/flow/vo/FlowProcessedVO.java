package com.officeflow.flow.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FlowProcessedVO {
    private Long id;
    private String applyNo;
    private String applyType;
    private String title;
    private String status;
    private Long applicantId;
    private String applicantName;
    private String myAction;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}

package com.officeflow.api.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogVO {
    private Long id;
    private Long userId;
    private String username;
    private String moduleName;
    private String operationType;
    private String requestMethod;
    private String requestPath;
    private Integer success;
    private LocalDateTime createdAt;
}

package com.officeflow.api.user.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}

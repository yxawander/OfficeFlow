package com.officeflow.api.user.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginLogVO {
    private Long id;
    private Long userId;
    private String username;
    private String loginIp;
    private String loginStatus;
    private String message;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}

package com.officeflow.api.user.vo;

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
    private LocalDateTime createdAt;
}

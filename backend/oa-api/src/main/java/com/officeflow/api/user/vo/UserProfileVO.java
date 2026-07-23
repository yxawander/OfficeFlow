package com.officeflow.api.user.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserProfileVO {
    private Long id;
    private String username;
    private String realName;
    private Integer gender;
    private String phone;
    private String email;
    private String avatar;
    private Long deptId;
    private String deptName;
    private Long postId;
    private String postName;
    private Long managerId;
    private String managerName;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate hireDate;
    private String userType;
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;
}

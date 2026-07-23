package com.officeflow.api.user.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private Integer gender;
    private String phone;
    private String email;
    private Long deptId;
    private String deptName;
    private Long postId;
    private String postName;
    private Long managerId;
    private String managerName;
    private LocalDate hireDate;
    private String userType;
    private Integer status;
    private LocalDateTime createdAt;
}

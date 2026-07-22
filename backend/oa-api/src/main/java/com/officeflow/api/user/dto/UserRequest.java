package com.officeflow.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserRequest {
    @NotBlank(message = "账号不能为空")
    private String username;

    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    private Integer gender;
    private String phone;
    private String email;
    private String avatar;
    private Long deptId;
    private Long postId;
    private Long managerId;
    private LocalDate hireDate;
    private String userType;
    private Integer status;
}

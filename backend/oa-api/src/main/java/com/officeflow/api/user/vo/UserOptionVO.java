package com.officeflow.api.user.vo;

import lombok.Data;

@Data
public class UserOptionVO {
    private Long id;
    private String username;
    private String realName;
    private Long deptId;
    private Long postId;
    private Long managerId;
    private Integer status;
}

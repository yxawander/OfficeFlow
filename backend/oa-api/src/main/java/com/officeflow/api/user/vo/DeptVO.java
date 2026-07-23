package com.officeflow.api.user.vo;

import lombok.Data;

@Data
public class DeptVO {
    private Long id;
    private Long parentId;
    private String deptName;
    private String deptCode;
    private Long leaderId;
    private String leaderName;
    private String phone;
    private String email;
    private Integer sortOrder;
    private Integer status;
}

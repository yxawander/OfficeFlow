package com.officeflow.notice.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class NoticeReadDetailVO {
    private Long noticeId;
    private Integer totalUsers;
    private Integer readUsers;
    private Integer unreadUsers;
    private BigDecimal readRate;
    private List<DeptStatVO> deptStats;

    @Data
    public static class DeptStatVO {
        private Long deptId;
        private String deptName;
        private Integer totalUsers;
        private Integer readUsers;
        private Integer unreadUsers;
        private BigDecimal readRate;
    }
}
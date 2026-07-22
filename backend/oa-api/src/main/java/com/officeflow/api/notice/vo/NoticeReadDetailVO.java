package com.officeflow.api.notice.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class NoticeReadDetailVO {
    private Long noticeId;
    private Long totalUsers;
    private Long readUsers;
    private Long unreadUsers;
    private BigDecimal readRate;
    private List<DeptStatVO> deptStats;

    @Data
    public static class DeptStatVO {
        private Long deptId;
        private String deptName;
        private Long totalUsers;
        private Long readUsers;
        private Long unreadUsers;
        private BigDecimal readRate;
    }
}
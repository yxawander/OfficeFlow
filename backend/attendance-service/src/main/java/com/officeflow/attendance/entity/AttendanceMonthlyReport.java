package com.officeflow.attendance.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AttendanceMonthlyReport {
    private Long id;
    private Long userId;
    private Long deptId;
    private String reportMonth; // YYYY-MM
    private Integer shouldWorkDays;
    private Integer actualWorkDays;
    private Integer lateCount;
    private Integer earlyLeaveCount;
    private Integer absentCount;
    private Integer missingCardCount;
    private BigDecimal leaveDays;
    private BigDecimal overtimeHours;
    private LocalDateTime generatedAt;

    // Extra fields for VO display
    private String username;
    private String realName;
    private String deptName;
}

package com.officeflow.attendance.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AttendanceRecord {
    private Long id;
    private Long userId;
    private Long deptId;
    private LocalDate workDate;
    private LocalDateTime checkInTime;
    private String checkInIp;
    private String checkInRemark;
    private LocalDateTime checkOutTime;
    private String checkOutIp;
    private String checkOutRemark;
    private Integer workMinutes;
    private Integer lateMinutes;
    private Integer earlyLeaveMinutes;
    private String status; // NORMAL, LATE, EARLY_LEAVE, ABSENT, MISSING_CARD
    private String source; // USER_CHECK, MANUAL
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

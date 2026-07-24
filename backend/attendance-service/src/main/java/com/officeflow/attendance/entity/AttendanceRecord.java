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
    private Double checkInLatitude;
    private Double checkInLongitude;
    private Double checkInAccuracyMeters;
    private Integer checkInDistanceMeters;
    private String checkInLocationName;
    private LocalDateTime checkOutTime;
    private String checkOutIp;
    private String checkOutRemark;
    private Double checkOutLatitude;
    private Double checkOutLongitude;
    private Double checkOutAccuracyMeters;
    private Integer checkOutDistanceMeters;
    private String checkOutLocationName;
    private Integer workMinutes;
    private Integer lateMinutes;
    private Integer earlyLeaveMinutes;
    private Integer overtimeMinutes;
    private Integer leaveMinutes;
    private Integer isMissingCard;
    private Integer isAbsent;
    
    // 状态 (NORMAL, LATE, EARLY_LEAVE, LATE_AND_EARLY, ABSENT, MISSING_CARD, RECHECKED)
    private String status;
    /** 异常处理状态(NORMAL, PENDING_APPEAL, LOCKED_ABSENT) */
    private String exceptionFlag;
    private String source; // USER_CHECK, MANUAL
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.officeflow.attendance.dto;

import java.time.LocalDateTime;

public record TodayAttendanceResponse(
        boolean hasCheckIn,
        LocalDateTime checkInTime,
        boolean hasCheckOut,
        LocalDateTime checkOutTime,
        int workMinutes,
        int lateMinutes,
        int earlyLeaveMinutes,
        String status,
        /** 异常处理状态(NORMAL, PENDING_APPEAL, LOCKED_ABSENT) */
        String exceptionFlag
) {
}

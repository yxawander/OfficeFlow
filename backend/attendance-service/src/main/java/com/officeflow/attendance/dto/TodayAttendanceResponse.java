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
        String status
) {
}

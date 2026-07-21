package com.officeflow.attendance.dto;

public record AttendanceRuleRequest(
        String ruleName,
        String workStartTime,
        String workEndTime,
        Integer lateThresholdMinutes,
        Integer earlyLeaveThresholdMinutes,
        Integer absentThresholdMinutes
) {
}

package com.officeflow.attendance.dto;

public record AttendanceGroupRequest(
        String groupName,
        Long ruleId,
        Long deptId
) {
}

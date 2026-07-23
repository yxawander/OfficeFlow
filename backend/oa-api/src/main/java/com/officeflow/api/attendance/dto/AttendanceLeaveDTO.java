package com.officeflow.api.attendance.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AttendanceLeaveDTO {
    private Long userId;
    private Long deptId;
    private LocalDate workDate;
}

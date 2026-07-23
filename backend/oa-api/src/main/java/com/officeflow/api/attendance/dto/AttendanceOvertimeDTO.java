package com.officeflow.api.attendance.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AttendanceOvertimeDTO {
    private Long userId;
    private Long deptId;
    private LocalDate workDate;
    private java.math.BigDecimal durationHours; // in hours, usually converted to minutes later
}

package com.officeflow.attendance.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttendanceCorrectionApply {
    private Long id;
    private Long userId;
    private Long attendanceRecordId;
    private String correctionType; // CHECK_IN, CHECK_OUT

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime correctionTime;

    private String reason;
    private Long flowApplyId;
    private String status; // PENDING, APPROVED, REJECTED, CANCELED

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}

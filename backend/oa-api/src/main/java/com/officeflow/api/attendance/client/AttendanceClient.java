package com.officeflow.api.attendance.client;

import com.officeflow.api.attendance.dto.AttendanceCorrectionDTO;
import com.officeflow.api.attendance.dto.AttendanceLeaveDTO;
import com.officeflow.api.attendance.dto.AttendanceOvertimeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(contextId = "attendanceClient", value = "attendance-service")
public interface AttendanceClient {

    @PostMapping("/api/inner/attendance/correction")
    com.officeflow.common.api.ApiResponse<Void> updateAttendanceForCorrection(@RequestBody AttendanceCorrectionDTO dto);

    @PostMapping("/api/inner/attendance/leave")
    com.officeflow.common.api.ApiResponse<Void> updateAttendanceForLeave(@RequestBody AttendanceLeaveDTO dto);

    @PostMapping("/api/inner/attendance/overtime")
    com.officeflow.common.api.ApiResponse<Void> updateAttendanceForOvertime(@RequestBody AttendanceOvertimeDTO dto);
}

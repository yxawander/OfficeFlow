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
    void updateAttendanceForCorrection(@RequestBody AttendanceCorrectionDTO dto);

    @PostMapping("/api/inner/attendance/leave")
    void updateAttendanceForLeave(@RequestBody AttendanceLeaveDTO dto);

    @PostMapping("/api/inner/attendance/overtime")
    void updateAttendanceForOvertime(@RequestBody AttendanceOvertimeDTO dto);
}

package com.officeflow.attendance.controller.inner;

import com.officeflow.api.attendance.dto.AttendanceCorrectionDTO;
import com.officeflow.api.attendance.dto.AttendanceLeaveDTO;
import com.officeflow.api.attendance.dto.AttendanceOvertimeDTO;
import com.officeflow.attendance.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inner/attendance")
public class AttendanceInnerController {

    @Autowired
    private AttendanceService attendanceService;

    @PostMapping("/correction")
    public com.officeflow.common.api.ApiResponse<Void> updateAttendanceForCorrection(@RequestBody AttendanceCorrectionDTO dto) {
        attendanceService.processCorrectionApprove(dto);
        return com.officeflow.common.api.ApiResponse.ok();
    }

    @PostMapping("/leave")
    public com.officeflow.common.api.ApiResponse<Void> updateAttendanceForLeave(@RequestBody AttendanceLeaveDTO dto) {
        attendanceService.processLeaveApprove(dto);
        return com.officeflow.common.api.ApiResponse.ok();
    }

    @PostMapping("/overtime")
    public com.officeflow.common.api.ApiResponse<Void> updateAttendanceForOvertime(@RequestBody AttendanceOvertimeDTO dto) {
        attendanceService.processOvertimeApprove(dto);
        return com.officeflow.common.api.ApiResponse.ok();
    }
}

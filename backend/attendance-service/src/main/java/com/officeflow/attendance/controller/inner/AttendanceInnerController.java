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
    public void updateAttendanceForCorrection(@RequestBody AttendanceCorrectionDTO dto) {
        attendanceService.processCorrectionApprove(dto);
    }

    @PostMapping("/leave")
    public void updateAttendanceForLeave(@RequestBody AttendanceLeaveDTO dto) {
        attendanceService.processLeaveApprove(dto);
    }

    @PostMapping("/overtime")
    public void updateAttendanceForOvertime(@RequestBody AttendanceOvertimeDTO dto) {
        attendanceService.processOvertimeApprove(dto);
    }
}

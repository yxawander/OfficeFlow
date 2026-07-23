package com.officeflow.attendance.controller;

import com.officeflow.attendance.dto.CheckInRequest;
import com.officeflow.attendance.dto.CheckOutRequest;
import com.officeflow.attendance.dto.TodayAttendanceResponse;
import com.officeflow.attendance.entity.AttendanceRecord;
import com.officeflow.attendance.service.AttendanceService;
import com.officeflow.attendance.util.RequestUser;
import com.officeflow.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /**
     * 上班打卡
     */
    @PostMapping("/check-in")
    public ApiResponse<AttendanceRecord> checkIn(@RequestBody(required = false) CheckInRequest request,
                                                  HttpServletRequest httpRequest) {
        Long userId = RequestUser.userId(httpRequest);
        return ApiResponse.ok(attendanceService.checkIn(userId, request, httpRequest));
    }

    /**
     * 下班打卡
     */
    @PostMapping("/check-out")
    public ApiResponse<AttendanceRecord> checkOut(@RequestBody(required = false) CheckOutRequest request,
                                                    HttpServletRequest httpRequest) {
        Long userId = RequestUser.userId(httpRequest);
        return ApiResponse.ok(attendanceService.checkOut(userId, request, httpRequest));
    }

    /**
     * 提交补卡申请
     */
    @PostMapping("/recheck")
    public ApiResponse<Void> recheck(@RequestBody com.officeflow.attendance.dto.CorrectionRequest request,
                                     HttpServletRequest httpRequest) {
        Long userId = RequestUser.userId(httpRequest);
        attendanceService.recheck(userId, request);
        return ApiResponse.ok();
    }

    /**
     * 查询个人补卡申请历史
     */
    @GetMapping("/corrections")
    public ApiResponse<java.util.List<com.officeflow.attendance.entity.AttendanceCorrectionApply>> myCorrections(HttpServletRequest httpRequest) {
        Long userId = RequestUser.userId(httpRequest);
        return ApiResponse.ok(attendanceService.getMyCorrections(userId));
    }

    /**
     * 获取当前登录用户今日打卡状态
     */
    @GetMapping("/today")
    public ApiResponse<TodayAttendanceResponse> todayStatus(HttpServletRequest httpRequest) {
        Long userId = RequestUser.userId(httpRequest);
        return ApiResponse.ok(attendanceService.getTodayStatus(userId));
    }

    /**
     * 获取当前登录用户适用的定位打卡配置
     */
    @GetMapping("/location-config")
    public ApiResponse<Map<String, Object>> locationConfig(HttpServletRequest httpRequest) {
        Long userId = RequestUser.userId(httpRequest);
        return ApiResponse.ok(attendanceService.getLocationConfig(userId));
    }

    /**
     * 查询个人历史考勤记录
     */
    @GetMapping("/my-records")
    public ApiResponse<Map<String, Object>> myRecords(@RequestParam(name = "startDate", required = false) String startDate,
                                                      @RequestParam(name = "endDate", required = false) String endDate,
                                                      @RequestParam(name = "page", defaultValue = "1") Integer page,
                                                      @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                      HttpServletRequest httpRequest) {
        Long userId = RequestUser.userId(httpRequest);
        return ApiResponse.ok(attendanceService.getMyRecords(userId, startDate, endDate, page, pageSize));
    }

    /**
     * 部门今日考勤实时监控
     */
    @GetMapping("/dept-today")
    public ApiResponse<Map<String, Object>> deptToday(@RequestParam(name = "deptId", required = false) Long deptId,
                                                      HttpServletRequest httpRequest) {
        Long userId = RequestUser.userId(httpRequest);
        return ApiResponse.ok(attendanceService.getDeptTodayOverview(userId, deptId));
    }

    /**
     * 查询所有考勤规则列表
     */
    @GetMapping("/rules")
    public ApiResponse<java.util.List<Map<String, Object>>> getRules() {
        return ApiResponse.ok(attendanceService.getAllRules());
    }

    /**
     * 管理员创建新考勤规则
     */
    @PostMapping("/rules")
    public ApiResponse<Void> createRule(@RequestBody com.officeflow.attendance.dto.AttendanceRuleRequest request) {
        attendanceService.createRule(request);
        return ApiResponse.ok();
    }

    /**
     * 管理员修改考勤规则参数（上班时间、迟到缓冲等）
     */
    @org.springframework.web.bind.annotation.PutMapping("/rules/{id}")
    public ApiResponse<Void> updateRule(@org.springframework.web.bind.annotation.PathVariable(name = "id") Long id,
                                        @RequestBody com.officeflow.attendance.dto.AttendanceRuleRequest request) {
        attendanceService.updateRule(id, request);
        return ApiResponse.ok();
    }

    /**
     * 查询考勤组及部门绑定关系列表
     */
    @GetMapping("/groups")
    public ApiResponse<java.util.List<Map<String, Object>>> getGroups() {
        return ApiResponse.ok(attendanceService.getAllGroups());
    }

    /**
     * 新增考勤组（绑定部门与规则）
     */
    @PostMapping("/groups")
    public ApiResponse<Void> createGroup(@RequestBody com.officeflow.attendance.dto.AttendanceGroupRequest request) {
        attendanceService.createGroup(request);
        return ApiResponse.ok();
    }

    /**
     * 修改考勤组（更新部门与规则绑定）
     */
    @org.springframework.web.bind.annotation.PutMapping("/groups/{id}")
    public ApiResponse<Void> updateGroup(@org.springframework.web.bind.annotation.PathVariable(name = "id") Long id,
                                         @RequestBody com.officeflow.attendance.dto.AttendanceGroupRequest request) {
        attendanceService.updateGroup(id, request);
        return ApiResponse.ok();
    }
}

package com.officeflow.attendance.service;

import com.officeflow.attendance.dto.CheckInRequest;
import com.officeflow.attendance.dto.CheckOutRequest;
import com.officeflow.attendance.dto.TodayAttendanceResponse;
import com.officeflow.attendance.entity.AttendanceRecord;
import com.officeflow.attendance.mapper.AttendanceRecordMapper;
import com.officeflow.attendance.mapper.AttendanceRuleMapper;
import com.officeflow.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceService {

    private final AttendanceRecordMapper attendanceRecordMapper;
    private final AttendanceRuleMapper attendanceRuleMapper;
    private final com.officeflow.attendance.mapper.AttendanceGroupMapper attendanceGroupMapper;

    // 规定上班时间：09:00
    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);
    // 迟到容忍门槛：10 分钟（09:10 之后算迟到）
    private static final int LATE_THRESHOLD_MINUTES = 10;
    // 规定下班时间：18:00
    private static final LocalTime WORK_END_TIME = LocalTime.of(18, 0);
    // 早退容忍门槛：10 分钟
    private static final int EARLY_LEAVE_THRESHOLD_MINUTES = 10;

    public AttendanceService(AttendanceRecordMapper attendanceRecordMapper,
                             AttendanceRuleMapper attendanceRuleMapper,
                             com.officeflow.attendance.mapper.AttendanceGroupMapper attendanceGroupMapper) {
        this.attendanceRecordMapper = attendanceRecordMapper;
        this.attendanceRuleMapper = attendanceRuleMapper;
        this.attendanceGroupMapper = attendanceGroupMapper;
    }

    @Transactional
    public AttendanceRecord checkIn(Long userId, CheckInRequest request, HttpServletRequest httpRequest) {
        if (userId == null) {
            throw new BusinessException("未获取到当前登录用户");
        }
        LocalDate today = LocalDate.now();
        AttendanceRecord existing = attendanceRecordMapper.findByUserIdAndWorkDate(userId, today);
        if (existing != null && existing.getCheckInTime() != null) {
            throw new BusinessException("今日已完成上班打卡，请勿重复打卡");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();

        Long deptId = attendanceRecordMapper.findDeptIdByUserId(userId);
        Map<String, Object> rule = getEffectiveRule(deptId);

        LocalTime workStartTime = parseTime(rule.get("workStartTime"), WORK_START_TIME);
        int lateThresholdMinutes = parseInt(rule.get("lateThresholdMinutes"), LATE_THRESHOLD_MINUTES);
        int absentThresholdMinutes = parseInt(rule.get("absentThresholdMinutes"), 240);

        int lateMinutes = 0;
        String status = "NORMAL";

        // 动态计算迟到与旷工：超过 数据库配置的上班时间 + 迟到缓冲分钟
        if (currentTime.isAfter(workStartTime.plusMinutes(lateThresholdMinutes))) {
            lateMinutes = (int) Duration.between(workStartTime, currentTime).toMinutes();
            if (lateMinutes >= absentThresholdMinutes) {
                status = "ABSENT"; // 超过旷工判定门槛（如4小时/240分钟），直接标记为旷工！
            } else {
                status = "LATE";
            }
        }

        AttendanceRecord record = new AttendanceRecord();
        record.setUserId(userId);
        record.setDeptId(deptId);
        record.setWorkDate(today);
        record.setCheckInTime(now);
        record.setCheckInIp(getClientIp(httpRequest));
        record.setCheckInRemark(request != null ? request.remark() : "");
        record.setLateMinutes(lateMinutes);
        record.setStatus(status);
        record.setSource("USER_CHECK");

        attendanceRecordMapper.insertCheckIn(record);
        return record;
    }

    @Transactional
    public AttendanceRecord checkOut(Long userId, CheckOutRequest request, HttpServletRequest httpRequest) {
        if (userId == null) {
            throw new BusinessException("未获取到当前登录用户");
        }
        LocalDate today = LocalDate.now();
        AttendanceRecord existing = attendanceRecordMapper.findByUserIdAndWorkDate(userId, today);
        if (existing == null || existing.getCheckInTime() == null) {
            throw new BusinessException("请先进行上班打卡再进行下班打卡");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();

        Long deptId = attendanceRecordMapper.findDeptIdByUserId(userId);
        Map<String, Object> rule = getEffectiveRule(deptId);

        LocalTime workEndTime = parseTime(rule.get("workEndTime"), WORK_END_TIME);
        int earlyLeaveThresholdMinutes = parseInt(rule.get("earlyLeaveThresholdMinutes"), EARLY_LEAVE_THRESHOLD_MINUTES);

        // 计算工作时长
        int workMinutes = (int) Duration.between(existing.getCheckInTime(), now).toMinutes();
        int earlyLeaveMinutes = 0;
        String status = existing.getStatus();

        // 动态判断早退（若已被判定为旷工，则保持旷工状态）
        if ("ABSENT".equals(status)) {
            // 保持旷工状态
        } else if (currentTime.isBefore(workEndTime.minusMinutes(earlyLeaveThresholdMinutes))) {
            earlyLeaveMinutes = (int) Duration.between(currentTime, workEndTime).toMinutes();
            if (!"LATE".equals(status)) {
                status = "EARLY_LEAVE";
            }
        }

        existing.setCheckOutTime(now);
        existing.setCheckOutIp(getClientIp(httpRequest));
        existing.setCheckOutRemark(request != null ? request.remark() : "");
        existing.setWorkMinutes(workMinutes);
        existing.setEarlyLeaveMinutes(earlyLeaveMinutes);
        existing.setStatus(status);

        attendanceRecordMapper.updateCheckOut(existing);
        return existing;
    }

    public TodayAttendanceResponse getTodayStatus(Long userId) {
        if (userId == null) {
            return new TodayAttendanceResponse(false, null, false, null, 0, 0, 0, "NORMAL");
        }
        LocalDate today = LocalDate.now();
        AttendanceRecord record = attendanceRecordMapper.findByUserIdAndWorkDate(userId, today);

        if (record == null) {
            return new TodayAttendanceResponse(false, null, false, null, 0, 0, 0, "NORMAL");
        }

        boolean hasCheckIn = record.getCheckInTime() != null;
        boolean hasCheckOut = record.getCheckOutTime() != null;

        return new TodayAttendanceResponse(
                hasCheckIn,
                record.getCheckInTime(),
                hasCheckOut,
                record.getCheckOutTime(),
                record.getWorkMinutes() != null ? record.getWorkMinutes() : 0,
                record.getLateMinutes() != null ? record.getLateMinutes() : 0,
                record.getEarlyLeaveMinutes() != null ? record.getEarlyLeaveMinutes() : 0,
                record.getStatus()
        );
    }

    public Map<String, Object> getMyRecords(Long userId, String startDate, String endDate, Integer page, Integer pageSize) {
        if (userId == null) {
            throw new BusinessException("未获取到当前登录用户");
        }
        int pageNo = (page == null || page < 1) ? 1 : page;
        int size = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        long offset = (long) (pageNo - 1) * size;

        long total = attendanceRecordMapper.countMyRecords(userId, startDate, endDate);
        List<Map<String, Object>> list = attendanceRecordMapper.listMyRecords(userId, startDate, endDate, offset, size);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", pageNo);
        result.put("pageSize", size);
        result.put("list", list);
        return result;
    }

    public Map<String, Object> getDeptTodayOverview(Long userId, Long targetDeptId) {
        Long deptId = targetDeptId;
        if (deptId == null && userId != null) {
            deptId = attendanceRecordMapper.findDeptIdByUserId(userId);
        }
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> list = attendanceRecordMapper.listDeptTodayRecords(deptId, today);

        int totalUsers = list.size();
        int checkedInUsers = 0;
        int lateUsers = 0;
        int notCheckedUsers = 0;

        for (Map<String, Object> row : list) {
            String status = (String) row.get("status");
            if ("NOT_CHECKED".equals(status)) {
                notCheckedUsers++;
            } else {
                checkedInUsers++;
                if ("LATE".equals(status)) {
                    lateUsers++;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("deptId", deptId);
        result.put("todayDate", today.toString());
        result.put("totalUsers", totalUsers);
        result.put("checkedInUsers", checkedInUsers);
        result.put("lateUsers", lateUsers);
        result.put("notCheckedUsers", notCheckedUsers);
        result.put("list", list);
        return result;
    }

    public List<Map<String, Object>> getAllRules() {
        return attendanceRuleMapper.selectAllRules();
    }

    @Transactional
    public void createRule(com.officeflow.attendance.dto.AttendanceRuleRequest request) {
        if (request == null || request.ruleName() == null || request.ruleName().isBlank()) {
            throw new BusinessException("规则名称不能为空");
        }
        attendanceRuleMapper.insertRule(request);
    }

    @Transactional
    public void updateRule(Long id, com.officeflow.attendance.dto.AttendanceRuleRequest request) {
        if (id == null) {
            throw new BusinessException("规则ID不能为空");
        }
        Map<String, Object> existing = attendanceRuleMapper.selectRuleById(id);
        if (existing == null) {
            throw new BusinessException("指定考勤规则不存在");
        }
        attendanceRuleMapper.updateRule(id, request);
    }

    public List<Map<String, Object>> getAllGroups() {
        return attendanceGroupMapper.selectAllGroups();
    }

    @Transactional
    public void createGroup(com.officeflow.attendance.dto.AttendanceGroupRequest request) {
        if (request == null || request.groupName() == null || request.groupName().isBlank()) {
            throw new BusinessException("考勤组名称不能为空");
        }
        if (request.ruleId() == null) {
            throw new BusinessException("请选择关联的考勤规则");
        }
        attendanceGroupMapper.insertGroup(request);
    }

    @Transactional
    public void updateGroup(Long id, com.officeflow.attendance.dto.AttendanceGroupRequest request) {
        if (id == null) {
            throw new BusinessException("考勤组ID不能为空");
        }
        Map<String, Object> existing = attendanceGroupMapper.selectGroupById(id);
        if (existing == null) {
            throw new BusinessException("指定考勤组不存在");
        }
        attendanceGroupMapper.updateGroup(id, request);
    }

    private Map<String, Object> getEffectiveRule(Long deptId) {
        Map<String, Object> rule = attendanceRuleMapper.selectRuleByDeptOrDefault(deptId);
        if (rule == null) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("workStartTime", "09:00:00");
            fallback.put("workEndTime", "18:00:00");
            fallback.put("lateThresholdMinutes", 10);
            fallback.put("earlyLeaveThresholdMinutes", 10);
            return fallback;
        }
        return rule;
    }

    private LocalTime parseTime(Object obj, LocalTime fallback) {
        if (obj == null) return fallback;
        try {
            return LocalTime.parse(obj.toString());
        } catch (Exception e) {
            return fallback;
        }
    }

    private int parseInt(Object obj, int fallback) {
        if (obj == null) return fallback;
        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
            return fallback;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}

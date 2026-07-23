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
    private final com.officeflow.attendance.mapper.AttendanceCorrectionApplyMapper attendanceCorrectionApplyMapper;

    // 规定上班时间：09:00
    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);
    // 迟到容忍门槛：10 分钟（09:10 之后算迟到）
    private static final int LATE_THRESHOLD_MINUTES = 10;
    // 规定下班时间：18:00
    private static final LocalTime WORK_END_TIME = LocalTime.of(18, 0);
    // 早退容忍门槛：10 分钟
    private static final int EARLY_LEAVE_THRESHOLD_MINUTES = 10;
    private static final int DEFAULT_ALLOWED_RADIUS_METERS = 1000;
    private static final int DEFAULT_ACCURACY_THRESHOLD_METERS = 1000;
    private static final double EARTH_RADIUS_METERS = 6371008.8;

    public AttendanceService(AttendanceRecordMapper attendanceRecordMapper,
                             AttendanceRuleMapper attendanceRuleMapper,
                             com.officeflow.attendance.mapper.AttendanceGroupMapper attendanceGroupMapper,
                             com.officeflow.attendance.mapper.AttendanceCorrectionApplyMapper attendanceCorrectionApplyMapper) {
        this.attendanceRecordMapper = attendanceRecordMapper;
        this.attendanceRuleMapper = attendanceRuleMapper;
        this.attendanceGroupMapper = attendanceGroupMapper;
        this.attendanceCorrectionApplyMapper = attendanceCorrectionApplyMapper;
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
        LocationCheck location = validateLocation(rule,
                request != null ? request.latitude() : null,
                request != null ? request.longitude() : null,
                request != null ? request.accuracyMeters() : null);

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
        record.setCheckInLatitude(location.latitude());
        record.setCheckInLongitude(location.longitude());
        record.setCheckInAccuracyMeters(location.accuracyMeters());
        record.setCheckInDistanceMeters(location.distanceMeters());
        record.setCheckInLocationName(location.locationName());
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
        LocationCheck location = validateLocation(rule,
                request != null ? request.latitude() : null,
                request != null ? request.longitude() : null,
                request != null ? request.accuracyMeters() : null);

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
            if ("LATE".equals(status)) {
                status = "LATE_AND_EARLY"; // 既迟到又早退！
            } else if (!"LATE_AND_EARLY".equals(status)) {
                status = "EARLY_LEAVE";
            }
        }

        existing.setCheckOutTime(now);
        existing.setCheckOutIp(getClientIp(httpRequest));
        existing.setCheckOutRemark(request != null ? request.remark() : "");
        existing.setCheckOutLatitude(location.latitude());
        existing.setCheckOutLongitude(location.longitude());
        existing.setCheckOutAccuracyMeters(location.accuracyMeters());
        existing.setCheckOutDistanceMeters(location.distanceMeters());
        existing.setCheckOutLocationName(location.locationName());
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

    public Map<String, Object> getLocationConfig(Long userId) {
        if (userId == null) {
            throw new BusinessException("未获取到当前登录用户");
        }
        Long deptId = attendanceRecordMapper.findDeptIdByUserId(userId);
        Map<String, Object> rule = getEffectiveRule(deptId);
        Map<String, Object> result = new HashMap<>();
        result.put("locationRequired", parseBoolean(rule.get("locationRequired"), false));
        result.put("officeLocationName", blankToDefault(parseString(rule.get("officeLocationName")), "办公地点"));
        result.put("officeAddress", parseString(rule.get("officeAddress")));
        result.put("officeLatitude", parseDouble(rule.get("officeLatitude")));
        result.put("officeLongitude", parseDouble(rule.get("officeLongitude")));
        result.put("allowedRadiusMeters", parseInt(rule.get("allowedRadiusMeters"), DEFAULT_ALLOWED_RADIUS_METERS));
        result.put("accuracyThresholdMeters", parseInt(rule.get("accuracyThresholdMeters"), DEFAULT_ACCURACY_THRESHOLD_METERS));
        result.put("locationConfigured", parseDouble(rule.get("officeLatitude")) != null && parseDouble(rule.get("officeLongitude")) != null);
        return result;
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
        attendanceRuleMapper.insertRule(normalizeRuleRequest(request));
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
        attendanceRuleMapper.updateRule(id, normalizeRuleRequest(request));
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

    @Transactional
    public void recheck(Long userId, com.officeflow.attendance.dto.CorrectionRequest request) {
        if (userId == null) {
            throw new BusinessException("未获取到当前登录用户");
        }
        if (request.correctionTime() == null) {
            throw new BusinessException("补卡时间不能为空");
        }
        if (request.correctionType() == null || request.reason() == null || request.reason().isBlank()) {
            throw new BusinessException("补卡类型和原因不能为空");
        }

        // 防重复与防止对已驳回记录重复申请
        LocalDate targetWorkDate = request.workDate() != null ? request.workDate() : request.correctionTime().toLocalDate();
        int existingCount = attendanceCorrectionApplyMapper.countActiveOrRejectedCorrection(userId, request.attendanceRecordId(), targetWorkDate);
        if (existingCount > 0) {
            throw new BusinessException("该日期已在审批中或已被驳回，不可重复发起补卡申请");
        }

        Long deptId = attendanceRecordMapper.findDeptIdByUserId(userId);
        Long managerId = attendanceCorrectionApplyMapper.selectManagerIdByUserId(userId);
        if (managerId == null) {
            managerId = 1L; // 默认超级管理员
        }

        // 1. 生成 flow_apply
        String applyNo = "CORR" + System.currentTimeMillis();
        com.officeflow.attendance.dto.FlowApplyInsertParams flowParams = new com.officeflow.attendance.dto.FlowApplyInsertParams();
        flowParams.setApplyNo(applyNo);
        flowParams.setApplicantId(userId);
        flowParams.setDeptId(deptId);
        flowParams.setApproverId(managerId);
        flowParams.setApplyType("CORRECTION");
        flowParams.setTitle("考勤补卡申请 (" + ("CHECK_IN".equalsIgnoreCase(request.correctionType()) ? "上班补卡" : "下班补卡") + ")");
        flowParams.setReason(request.reason());
        flowParams.setStartTime(request.correctionTime());
        flowParams.setEndTime(request.correctionTime());
        flowParams.setDurationHours(java.math.BigDecimal.ZERO);
        flowParams.setStatus("PENDING");
        flowParams.setCurrentNode("DIRECT_MANAGER");

        attendanceCorrectionApplyMapper.insertFlowApply(flowParams);
        attendanceCorrectionApplyMapper.insertApproveRecord(flowParams.getId(), userId);

        // 2. 生成 attendance_correction_apply 记录
        com.officeflow.attendance.entity.AttendanceCorrectionApply apply = new com.officeflow.attendance.entity.AttendanceCorrectionApply();
        apply.setUserId(userId);
        apply.setAttendanceRecordId(request.attendanceRecordId());
        apply.setCorrectionType(request.correctionType());
        apply.setCorrectionTime(request.correctionTime());
        apply.setReason(request.reason());
        apply.setFlowApplyId(flowParams.getId());
        apply.setStatus("PENDING");

        attendanceCorrectionApplyMapper.insert(apply);
    }

    public List<com.officeflow.attendance.entity.AttendanceCorrectionApply> getMyCorrections(Long userId) {
        return attendanceCorrectionApplyMapper.listByUserId(userId);
    }

    private Map<String, Object> getEffectiveRule(Long deptId) {
        Map<String, Object> rule = attendanceRuleMapper.selectRuleByDeptOrDefault(deptId);
        if (rule == null) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("workStartTime", "09:00:00");
            fallback.put("workEndTime", "18:00:00");
            fallback.put("lateThresholdMinutes", 10);
            fallback.put("earlyLeaveThresholdMinutes", 10);
            fallback.put("absentThresholdMinutes", 240);
            fallback.put("locationRequired", false);
            fallback.put("allowedRadiusMeters", DEFAULT_ALLOWED_RADIUS_METERS);
            fallback.put("accuracyThresholdMeters", DEFAULT_ACCURACY_THRESHOLD_METERS);
            return fallback;
        }
        return rule;
    }

    private com.officeflow.attendance.dto.AttendanceRuleRequest normalizeRuleRequest(com.officeflow.attendance.dto.AttendanceRuleRequest request) {
        Boolean locationRequired = request.locationRequired() != null && request.locationRequired();
        Integer allowedRadiusMeters = request.allowedRadiusMeters() == null ? DEFAULT_ALLOWED_RADIUS_METERS : request.allowedRadiusMeters();
        Integer accuracyThresholdMeters = request.accuracyThresholdMeters() == null ? DEFAULT_ACCURACY_THRESHOLD_METERS : request.accuracyThresholdMeters();

        if (allowedRadiusMeters <= 0) {
            throw new BusinessException("允许打卡半径必须大于0米");
        }
        if (accuracyThresholdMeters <= 0) {
            throw new BusinessException("定位精度阈值必须大于0米");
        }
        if (locationRequired && (request.officeLatitude() == null || request.officeLongitude() == null)) {
            throw new BusinessException("启用定位打卡时必须配置办公地点经纬度");
        }
        if (request.officeLatitude() != null && (request.officeLatitude() < -90 || request.officeLatitude() > 90)) {
            throw new BusinessException("办公地点纬度必须在 -90 到 90 之间");
        }
        if (request.officeLongitude() != null && (request.officeLongitude() < -180 || request.officeLongitude() > 180)) {
            throw new BusinessException("办公地点经度必须在 -180 到 180 之间");
        }

        return new com.officeflow.attendance.dto.AttendanceRuleRequest(
                request.ruleName(),
                request.workStartTime(),
                request.workEndTime(),
                request.lateThresholdMinutes() == null ? LATE_THRESHOLD_MINUTES : request.lateThresholdMinutes(),
                request.earlyLeaveThresholdMinutes() == null ? EARLY_LEAVE_THRESHOLD_MINUTES : request.earlyLeaveThresholdMinutes(),
                request.absentThresholdMinutes() == null ? 240 : request.absentThresholdMinutes(),
                locationRequired,
                blankToDefault(request.officeLocationName(), "默认办公点"),
                blankToDefault(request.officeAddress(), ""),
                request.officeLatitude(),
                request.officeLongitude(),
                allowedRadiusMeters,
                accuracyThresholdMeters
        );
    }

    private LocationCheck validateLocation(Map<String, Object> rule, Double latitude, Double longitude, Double accuracyMeters) {
        boolean required = parseBoolean(rule.get("locationRequired"), false);
        Double officeLatitude = parseDouble(rule.get("officeLatitude"));
        Double officeLongitude = parseDouble(rule.get("officeLongitude"));
        String locationName = blankToDefault(parseString(rule.get("officeLocationName")), "办公地点");
        int allowedRadiusMeters = parseInt(rule.get("allowedRadiusMeters"), DEFAULT_ALLOWED_RADIUS_METERS);
        int accuracyThresholdMeters = parseInt(rule.get("accuracyThresholdMeters"), DEFAULT_ACCURACY_THRESHOLD_METERS);

        if (!required && (latitude == null || longitude == null || officeLatitude == null || officeLongitude == null)) {
            return new LocationCheck(latitude, longitude, accuracyMeters, null, null);
        }
        if (latitude == null || longitude == null) {
            throw new BusinessException("请允许浏览器获取定位后再打卡");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BusinessException("定位经纬度不合法，请重新定位后再打卡");
        }
        if (accuracyMeters != null && accuracyMeters > accuracyThresholdMeters) {
            throw new BusinessException("当前定位精度约" + Math.round(accuracyMeters) + "米，超过允许精度" + accuracyThresholdMeters + "米，请移动到网络较好的位置后重试");
        }
        if (officeLatitude == null || officeLongitude == null) {
            throw new BusinessException("当前考勤规则未配置办公地点，请联系管理员维护考勤规则");
        }

        int distanceMeters = (int) Math.round(distanceMeters(latitude, longitude, officeLatitude, officeLongitude));
        if (required && distanceMeters > allowedRadiusMeters) {
            throw new BusinessException("当前位置距离" + locationName + "约" + distanceMeters + "米，超过允许打卡范围" + allowedRadiusMeters + "米");
        }
        return new LocationCheck(latitude, longitude, accuracyMeters, distanceMeters, locationName);
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private String parseString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private Double parseDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean parseBoolean(Object obj, boolean fallback) {
        if (obj == null) return fallback;
        if (obj instanceof Boolean bool) return bool;
        if (obj instanceof Number number) return number.intValue() != 0;
        String value = obj.toString();
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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

    private record LocationCheck(
            Double latitude,
            Double longitude,
            Double accuracyMeters,
            Integer distanceMeters,
            String locationName
    ) {
    }
}

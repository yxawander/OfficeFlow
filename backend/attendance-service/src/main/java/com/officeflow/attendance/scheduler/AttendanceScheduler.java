package com.officeflow.attendance.scheduler;

import com.officeflow.attendance.entity.AttendanceRecord;
import com.officeflow.attendance.mapper.AttendanceRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceScheduler {

    private final AttendanceRecordMapper attendanceRecordMapper;

    /**
     * 每天凌晨1点执行，扫描昨天的考勤记录
     * 如果昨天存在应出勤的用户但在 attendance_record 中不存在记录，则插入 ABSENT
     * 如果存在记录但没有下班卡且不是请假/旷工状态，则标记为 MISSING_CARD
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processMissingAttendance() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        // V2.0 合规级：周末不计入旷工/缺卡
        if (yesterday.getDayOfWeek() == java.time.DayOfWeek.SATURDAY || yesterday.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            log.info("昨日是周末 {}，跳过自动打标任务", yesterday);
            return;
        }

        log.info("开始执行考勤定时任务，处理日期：{}", yesterday);
        
        try {
            // 获取所有在职员工，找出昨天没有任何考勤记录的，标记为 ABSENT
            List<Map<String, Object>> allUsers = attendanceRecordMapper.listDeptTodayRecords(null, yesterday);
            for (Map<String, Object> user : allUsers) {
                Object recordId = user.get("recordId");
                if (recordId == null) {
                    Long userId = Long.parseLong(user.get("userId").toString());
                    Long deptId = user.get("deptId") != null ? Long.parseLong(user.get("deptId").toString()) : null;
                    
                    AttendanceRecord absentRecord = new AttendanceRecord();
                    absentRecord.setUserId(userId);
                    absentRecord.setDeptId(deptId);
                    absentRecord.setWorkDate(yesterday);
                    absentRecord.setStatus("ABSENT");
                    absentRecord.setSource("SYSTEM");
                    absentRecord.setIsAbsent(1);
                    absentRecord.setIsMissingCard(0);
                    absentRecord.setLeaveMinutes(0);
                    absentRecord.setLateMinutes(0);
                    absentRecord.setEarlyLeaveMinutes(0);
                    absentRecord.setWorkMinutes(0);
                    absentRecord.setExceptionFlag("PENDING_APPEAL");
                    
                    attendanceRecordMapper.insertSystemRecord(absentRecord);
                    log.info("用户 {} 昨日没有任何考勤记录，自动标记为旷工", userId);
                } else {
                    // 如果存在记录，检查是否缺下班卡
                    String status = (String) user.get("status");
                    Object checkOutTime = user.get("checkOutTime");
                    Object checkInTime = user.get("checkInTime");
                    int leaveMinutes = user.get("leaveMinutes") != null ? Integer.parseInt(user.get("leaveMinutes").toString()) : 0;
                    
                    // 如果休了全天假(>=480)，则忽略打卡
                    if (leaveMinutes >= 480) {
                        continue;
                    }

                    if ((checkOutTime == null || checkInTime == null) && !"ABSENT".equals(status)) {
                        // 标记为缺卡
                        Long id = Long.parseLong(recordId.toString());
                        Long userId = Long.parseLong(user.get("userId").toString());
                        
                        AttendanceRecord existing = attendanceRecordMapper.findById(id);
                        if (existing != null) {
                            existing.setStatus("MISSING_CARD");
                            existing.setIsMissingCard(1);
                            existing.setExceptionFlag("PENDING_APPEAL");
                            existing.setSource("SYSTEM");
                            attendanceRecordMapper.updateCheckOut(existing);
                            log.info("用户 {} 昨日缺下班卡，自动标记为缺卡", userId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("考勤定时任务执行异常", e);
        }
        
        log.info("考勤定时任务执行完毕");
    }
}

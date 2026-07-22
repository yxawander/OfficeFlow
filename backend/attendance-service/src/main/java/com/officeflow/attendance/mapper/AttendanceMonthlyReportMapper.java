package com.officeflow.attendance.mapper;

import com.officeflow.attendance.entity.AttendanceMonthlyReport;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface AttendanceMonthlyReportMapper {

    @Insert("""
            INSERT INTO attendance_monthly_report (user_id, dept_id, report_month, should_work_days, actual_work_days, late_count, early_leave_count, absent_count, missing_card_count, leave_days, overtime_hours, generated_at)
            VALUES (#{userId}, #{deptId}, #{reportMonth}, #{shouldWorkDays}, #{actualWorkDays}, #{lateCount}, #{earlyLeaveCount}, #{absentCount}, #{missingCardCount}, #{leaveDays}, #{overtimeHours}, NOW())
            ON DUPLICATE KEY UPDATE
                dept_id = VALUES(dept_id),
                should_work_days = VALUES(should_work_days),
                actual_work_days = VALUES(actual_work_days),
                late_count = VALUES(late_count),
                early_leave_count = VALUES(early_leave_count),
                absent_count = VALUES(absent_count),
                missing_card_count = VALUES(missing_card_count),
                leave_days = VALUES(leave_days),
                overtime_hours = VALUES(overtime_hours),
                generated_at = NOW()
            """)
    int upsertReport(AttendanceMonthlyReport report);

    @Select("""
            <script>
            SELECT r.id, r.user_id AS userId, r.dept_id AS deptId, r.report_month AS reportMonth,
                   r.should_work_days AS shouldWorkDays, r.actual_work_days AS actualWorkDays,
                   r.late_count AS lateCount, r.early_leave_count AS earlyLeaveCount,
                   r.absent_count AS absentCount, r.missing_card_count AS missingCardCount,
                   r.leave_days AS leaveDays, r.overtime_hours AS overtimeHours,
                   r.generated_at AS generatedAt,
                   u.real_name AS realName, u.username, d.dept_name AS deptName
            FROM attendance_monthly_report r
            LEFT JOIN sys_user u ON u.id = r.user_id
            LEFT JOIN sys_dept d ON d.id = r.dept_id
            WHERE r.report_month = #{reportMonth}
              <if test="deptId != null">AND r.dept_id = #{deptId}</if>
              <if test="keyword != null and keyword != ''">
                AND (u.real_name LIKE CONCAT('%', #{keyword}, '%') OR u.username LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            ORDER BY u.id ASC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<AttendanceMonthlyReport> selectReports(@Param("reportMonth") String reportMonth,
                                                @Param("deptId") Long deptId,
                                                @Param("keyword") String keyword,
                                                @Param("offset") long offset,
                                                @Param("pageSize") long pageSize);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM attendance_monthly_report r
            LEFT JOIN sys_user u ON u.id = r.user_id
            WHERE r.report_month = #{reportMonth}
              <if test="deptId != null">AND r.dept_id = #{deptId}</if>
              <if test="keyword != null and keyword != ''">
                AND (u.real_name LIKE CONCAT('%', #{keyword}, '%') OR u.username LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            </script>
            """)
    long countReports(@Param("reportMonth") String reportMonth,
                      @Param("deptId") Long deptId,
                      @Param("keyword") String keyword);

    @Select("SELECT u.id AS userId, u.dept_id AS deptId FROM sys_user u WHERE u.is_deleted = 0 AND u.status = 1")
    List<Map<String, Object>> listAllActiveUsers();

    @Select("""
            SELECT
              COUNT(DISTINCT CASE WHEN status IN ('NORMAL', 'LATE', 'EARLY_LEAVE', 'LATE_AND_EARLY', 'RECHECKED') THEN work_date END) AS actualWorkDays,
              SUM(CASE WHEN status IN ('LATE', 'LATE_AND_EARLY') THEN 1 ELSE 0 END) AS lateCount,
              SUM(CASE WHEN status IN ('EARLY_LEAVE', 'LATE_AND_EARLY') THEN 1 ELSE 0 END) AS earlyLeaveCount,
              SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END) AS absentCount,
              SUM(CASE WHEN status = 'MISSING_CARD' THEN 1 ELSE 0 END) AS missingCardCount
            FROM attendance_record
            WHERE user_id = #{userId} AND DATE_FORMAT(work_date, '%Y-%m') = #{reportMonth}
            """)
    Map<String, Object> calculateUserAttendanceStats(@Param("userId") Long userId, @Param("reportMonth") String reportMonth);

    @Select("""
            SELECT COALESCE(SUM(duration_hours), 0) / 8.0
            FROM flow_apply
            WHERE applicant_id = #{userId}
              AND apply_type = 'LEAVE'
              AND status = 'APPROVED'
              AND DATE_FORMAT(start_time, '%Y-%m') = #{reportMonth}
            """)
    BigDecimal calculateApprovedLeaveDays(@Param("userId") Long userId, @Param("reportMonth") String reportMonth);

    @Select("""
            SELECT duration_hours AS approvedHours, start_time AS startTime, end_time AS endTime
            FROM flow_apply
            WHERE applicant_id = #{userId}
              AND apply_type = 'OVERTIME'
              AND status = 'APPROVED'
              AND DATE_FORMAT(start_time, '%Y-%m') = #{reportMonth}
            """)
    List<Map<String, Object>> selectApprovedOvertimeApplies(@Param("userId") Long userId, @Param("reportMonth") String reportMonth);

    @Select("""
            SELECT TIMESTAMPDIFF(MINUTE, check_in_time, check_out_time) / 60.0 AS actualHours
            FROM attendance_record
            WHERE user_id = #{userId} AND work_date = DATE(#{workDate}) AND check_in_time IS NOT NULL AND check_out_time IS NOT NULL
            """)
    Double selectActualWorkHoursOnDate(@Param("userId") Long userId, @Param("workDate") Object workDate);
}

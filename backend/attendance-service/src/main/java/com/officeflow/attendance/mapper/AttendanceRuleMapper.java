package com.officeflow.attendance.mapper;

import com.officeflow.attendance.dto.AttendanceRuleRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface AttendanceRuleMapper {

    @Select("""
            SELECT id, rule_name AS ruleName, work_start_time AS workStartTime, work_end_time AS workEndTime,
                   late_threshold_minutes AS lateThresholdMinutes, early_leave_threshold_minutes AS earlyLeaveThresholdMinutes,
                   absent_threshold_minutes AS absentThresholdMinutes, status, created_at AS createdAt
            FROM attendance_rule
            ORDER BY id ASC
            """)
    List<Map<String, Object>> selectAllRules();

    @Select("""
            SELECT id, rule_name AS ruleName, work_start_time AS workStartTime, work_end_time AS workEndTime,
                   late_threshold_minutes AS lateThresholdMinutes, early_leave_threshold_minutes AS earlyLeaveThresholdMinutes,
                   absent_threshold_minutes AS absentThresholdMinutes, status
            FROM attendance_rule
            WHERE id = #{id}
            """)
    Map<String, Object> selectRuleById(@Param("id") Long id);

    @Insert("""
            INSERT INTO attendance_rule (rule_name, work_start_time, work_end_time, late_threshold_minutes, early_leave_threshold_minutes, absent_threshold_minutes, status)
            VALUES (#{req.ruleName}, #{req.workStartTime}, #{req.workEndTime}, #{req.lateThresholdMinutes}, #{req.earlyLeaveThresholdMinutes}, #{req.absentThresholdMinutes}, 1)
            """)
    int insertRule(@Param("req") AttendanceRuleRequest request);

    @Update("""
            UPDATE attendance_rule
            SET rule_name = #{req.ruleName},
                work_start_time = #{req.workStartTime},
                work_end_time = #{req.workEndTime},
                late_threshold_minutes = #{req.lateThresholdMinutes},
                early_leave_threshold_minutes = #{req.earlyLeaveThresholdMinutes},
                absent_threshold_minutes = #{req.absentThresholdMinutes}
            WHERE id = #{id}
            """)
    int updateRule(@Param("id") Long id, @Param("req") AttendanceRuleRequest request);

    @Select("""
            <script>
            SELECT r.id, r.rule_name AS ruleName, r.work_start_time AS workStartTime, r.work_end_time AS workEndTime,
                   r.late_threshold_minutes AS lateThresholdMinutes, r.early_leave_threshold_minutes AS earlyLeaveThresholdMinutes,
                   r.absent_threshold_minutes AS absentThresholdMinutes
            FROM attendance_rule r
            LEFT JOIN attendance_group g ON g.rule_id = r.id
            WHERE r.status = 1
              <if test="deptId != null">AND (g.dept_id = #{deptId} OR g.dept_id IS NULL)</if>
            ORDER BY g.dept_id DESC, r.id ASC
            LIMIT 1
            </script>
            """)
    Map<String, Object> selectRuleByDeptOrDefault(@Param("deptId") Long deptId);
}

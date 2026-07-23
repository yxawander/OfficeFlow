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
                   absent_threshold_minutes AS absentThresholdMinutes, check_lock_seconds AS checkLockSeconds,
                   location_required AS locationRequired, office_location_name AS officeLocationName,
                   office_address AS officeAddress, office_latitude AS officeLatitude, office_longitude AS officeLongitude,
                   allowed_radius_meters AS allowedRadiusMeters, accuracy_threshold_meters AS accuracyThresholdMeters,
                   status, created_at AS createdAt
            FROM attendance_rule
            ORDER BY id ASC
            """)
    List<Map<String, Object>> selectAllRules();

    @Select("""
            SELECT id, rule_name AS ruleName, work_start_time AS workStartTime, work_end_time AS workEndTime,
                   late_threshold_minutes AS lateThresholdMinutes, early_leave_threshold_minutes AS earlyLeaveThresholdMinutes,
                   absent_threshold_minutes AS absentThresholdMinutes, check_lock_seconds AS checkLockSeconds,
                   location_required AS locationRequired, office_location_name AS officeLocationName,
                   office_address AS officeAddress, office_latitude AS officeLatitude, office_longitude AS officeLongitude,
                   allowed_radius_meters AS allowedRadiusMeters, accuracy_threshold_meters AS accuracyThresholdMeters,
                   status
            FROM attendance_rule
            WHERE id = #{id}
            """)
    Map<String, Object> selectRuleById(@Param("id") Long id);

    @Insert("""
            INSERT INTO attendance_rule (
                rule_name, work_start_time, work_end_time, late_threshold_minutes, early_leave_threshold_minutes,
                absent_threshold_minutes, location_required, office_location_name, office_address,
                office_latitude, office_longitude, allowed_radius_meters, accuracy_threshold_meters, status
            )
            VALUES (
                #{req.ruleName}, #{req.workStartTime}, #{req.workEndTime}, #{req.lateThresholdMinutes}, #{req.earlyLeaveThresholdMinutes},
                #{req.absentThresholdMinutes}, #{req.locationRequired}, #{req.officeLocationName}, #{req.officeAddress},
                #{req.officeLatitude}, #{req.officeLongitude}, #{req.allowedRadiusMeters}, #{req.accuracyThresholdMeters}, 1
            )
            """)
    int insertRule(@Param("req") AttendanceRuleRequest request);

    @Update("""
            UPDATE attendance_rule
            SET rule_name = #{req.ruleName},
                work_start_time = #{req.workStartTime},
                work_end_time = #{req.workEndTime},
                late_threshold_minutes = #{req.lateThresholdMinutes},
                early_leave_threshold_minutes = #{req.earlyLeaveThresholdMinutes},
                absent_threshold_minutes = #{req.absentThresholdMinutes},
                location_required = #{req.locationRequired},
                office_location_name = #{req.officeLocationName},
                office_address = #{req.officeAddress},
                office_latitude = #{req.officeLatitude},
                office_longitude = #{req.officeLongitude},
                allowed_radius_meters = #{req.allowedRadiusMeters},
                accuracy_threshold_meters = #{req.accuracyThresholdMeters}
            WHERE id = #{id}
            """)
    int updateRule(@Param("id") Long id, @Param("req") AttendanceRuleRequest request);

    @Select("""
            <script>
            SELECT r.id, r.rule_name AS ruleName, r.work_start_time AS workStartTime, r.work_end_time AS workEndTime,
                   r.late_threshold_minutes AS lateThresholdMinutes, r.early_leave_threshold_minutes AS earlyLeaveThresholdMinutes,
                   r.absent_threshold_minutes AS absentThresholdMinutes, r.check_lock_seconds AS checkLockSeconds,
                   r.location_required AS locationRequired, r.office_location_name AS officeLocationName,
                   r.office_address AS officeAddress, r.office_latitude AS officeLatitude, r.office_longitude AS officeLongitude,
                   r.allowed_radius_meters AS allowedRadiusMeters, r.accuracy_threshold_meters AS accuracyThresholdMeters
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

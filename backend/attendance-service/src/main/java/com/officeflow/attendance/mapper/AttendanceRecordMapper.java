package com.officeflow.attendance.mapper;

import com.officeflow.attendance.entity.AttendanceRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface AttendanceRecordMapper {

    @Select("""
            SELECT id, user_id AS userId, dept_id AS deptId, work_date AS workDate,
                   check_in_time AS checkInTime, check_in_ip AS checkInIp, check_in_remark AS checkInRemark,
                   check_in_latitude AS checkInLatitude, check_in_longitude AS checkInLongitude,
                   check_in_accuracy_meters AS checkInAccuracyMeters, check_in_distance_meters AS checkInDistanceMeters,
                   check_in_location_name AS checkInLocationName,
                   check_out_time AS checkOutTime, check_out_ip AS checkOutIp, check_out_remark AS checkOutRemark,
                   check_out_latitude AS checkOutLatitude, check_out_longitude AS checkOutLongitude,
                   check_out_accuracy_meters AS checkOutAccuracyMeters, check_out_distance_meters AS checkOutDistanceMeters,
                   check_out_location_name AS checkOutLocationName,
                   work_minutes AS workMinutes, late_minutes AS lateMinutes, early_leave_minutes AS earlyLeaveMinutes,
                   status, source, created_at AS createdAt, updated_at AS updatedAt
            FROM attendance_record
            WHERE user_id = #{userId} AND work_date = #{workDate}
            """)
    AttendanceRecord findByUserIdAndWorkDate(@Param("userId") Long userId, @Param("workDate") LocalDate workDate);

    @Insert("""
            INSERT INTO attendance_record (
                user_id, dept_id, work_date, check_in_time, check_in_ip, check_in_remark,
                check_in_latitude, check_in_longitude, check_in_accuracy_meters, check_in_distance_meters, check_in_location_name,
                late_minutes, status, source
            )
            VALUES (
                #{userId}, #{deptId}, #{workDate}, #{checkInTime}, #{checkInIp}, #{checkInRemark},
                #{checkInLatitude}, #{checkInLongitude}, #{checkInAccuracyMeters}, #{checkInDistanceMeters}, #{checkInLocationName},
                #{lateMinutes}, #{status}, #{source}
            )
            """)
    int insertCheckIn(AttendanceRecord record);

    @Update("""
            UPDATE attendance_record
            SET check_out_time = #{checkOutTime},
                check_out_ip = #{checkOutIp},
                check_out_remark = #{checkOutRemark},
                check_out_latitude = #{checkOutLatitude},
                check_out_longitude = #{checkOutLongitude},
                check_out_accuracy_meters = #{checkOutAccuracyMeters},
                check_out_distance_meters = #{checkOutDistanceMeters},
                check_out_location_name = #{checkOutLocationName},
                work_minutes = #{workMinutes},
                early_leave_minutes = #{earlyLeaveMinutes},
                status = #{status}
            WHERE id = #{id}
            """)
    int updateCheckOut(AttendanceRecord record);

    @Select("""
            SELECT id, user_id AS userId, dept_id AS deptId, work_date AS workDate,
                   check_in_time AS checkInTime, check_in_ip AS checkInIp, check_in_remark AS checkInRemark,
                   check_in_latitude AS checkInLatitude, check_in_longitude AS checkInLongitude,
                   check_in_accuracy_meters AS checkInAccuracyMeters, check_in_distance_meters AS checkInDistanceMeters,
                   check_in_location_name AS checkInLocationName,
                   check_out_time AS checkOutTime, check_out_ip AS checkOutIp, check_out_remark AS checkOutRemark,
                   check_out_latitude AS checkOutLatitude, check_out_longitude AS checkOutLongitude,
                   check_out_accuracy_meters AS checkOutAccuracyMeters, check_out_distance_meters AS checkOutDistanceMeters,
                   check_out_location_name AS checkOutLocationName,
                   work_minutes AS workMinutes, late_minutes AS lateMinutes, early_leave_minutes AS earlyLeaveMinutes,
                   status, source, created_at AS createdAt, updated_at AS updatedAt
            FROM attendance_record
            WHERE id = #{id}
            """)
    AttendanceRecord findById(@Param("id") Long id);

    @Update("""
            UPDATE attendance_record
            SET check_in_time = #{checkInTime},
                check_in_remark = #{checkInRemark},
                check_out_time = #{checkOutTime},
                check_out_remark = #{checkOutRemark},
                work_minutes = #{workMinutes},
                late_minutes = #{lateMinutes},
                early_leave_minutes = #{earlyLeaveMinutes},
                status = #{status},
                source = #{source}
            WHERE id = #{id}
            """)
    int updateRecord(AttendanceRecord record);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM attendance_record r
            WHERE r.user_id = #{userId}
              <if test="startDate != null and startDate != ''">AND r.work_date &gt;= #{startDate}</if>
              <if test="endDate != null and endDate != ''">AND r.work_date &lt;= #{endDate}</if>
            </script>
            """)
    long countMyRecords(@Param("userId") Long userId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("""
            <script>
            SELECT r.id, r.user_id AS userId, r.work_date AS workDate,
                   r.check_in_time AS checkInTime, r.check_in_remark AS checkInRemark,
                   r.check_in_distance_meters AS checkInDistanceMeters, r.check_in_location_name AS checkInLocationName,
                   r.check_out_time AS checkOutTime, r.check_out_remark AS checkOutRemark,
                   r.check_out_distance_meters AS checkOutDistanceMeters, r.check_out_location_name AS checkOutLocationName,
                   r.work_minutes AS workMinutes, r.late_minutes AS lateMinutes, r.early_leave_minutes AS earlyLeaveMinutes,
                   r.status, r.source,
                   ca.status AS correctionStatus
            FROM attendance_record r
            LEFT JOIN attendance_correction_apply ca ON ca.id = (
                SELECT id FROM attendance_correction_apply
                WHERE user_id = r.user_id AND (attendance_record_id = r.id OR DATE(correction_time) = r.work_date)
                ORDER BY id DESC LIMIT 1
            )
            WHERE r.user_id = #{userId}
              <if test="startDate != null and startDate != ''">AND r.work_date &gt;= #{startDate}</if>
              <if test="endDate != null and endDate != ''">AND r.work_date &lt;= #{endDate}</if>
            ORDER BY r.work_date DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<Map<String, Object>> listMyRecords(@Param("userId") Long userId,
                                            @Param("startDate") String startDate,
                                            @Param("endDate") String endDate,
                                            @Param("offset") long offset,
                                            @Param("pageSize") long pageSize);

    @Select("SELECT dept_id FROM sys_user WHERE id = #{userId} AND is_deleted = 0")
    Long findDeptIdByUserId(@Param("userId") Long userId);

    @Select("""
            <script>
            SELECT u.id AS userId, u.real_name AS realName, u.username, d.dept_name AS deptName,
                   r.check_in_time AS checkInTime, r.check_in_remark AS checkInRemark,
                   r.check_in_distance_meters AS checkInDistanceMeters, r.check_in_location_name AS checkInLocationName,
                   r.check_out_time AS checkOutTime, r.check_out_remark AS checkOutRemark,
                   r.check_out_distance_meters AS checkOutDistanceMeters, r.check_out_location_name AS checkOutLocationName,
                   r.work_minutes AS workMinutes, r.late_minutes AS lateMinutes, r.early_leave_minutes AS earlyLeaveMinutes,
                   COALESCE(r.status, 'NOT_CHECKED') AS status
            FROM sys_user u
            LEFT JOIN sys_dept d ON d.id = u.dept_id
            LEFT JOIN attendance_record r ON r.user_id = u.id AND r.work_date = #{workDate}
            WHERE u.is_deleted = 0 AND u.status = 1
              <if test="deptId != null">AND u.dept_id = #{deptId}</if>
            ORDER BY u.id ASC
            </script>
            """)
    List<Map<String, Object>> listDeptTodayRecords(@Param("deptId") Long deptId, @Param("workDate") LocalDate workDate);
}

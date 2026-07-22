package com.officeflow.attendance.mapper;

import com.officeflow.attendance.entity.AttendanceCorrectionApply;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AttendanceCorrectionApplyMapper {

    @Insert("""
            INSERT INTO attendance_correction_apply (user_id, attendance_record_id, correction_type, correction_time, reason, flow_apply_id, status)
            VALUES (#{userId}, #{attendanceRecordId}, #{correctionType}, #{correctionTime}, #{reason}, #{flowApplyId}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AttendanceCorrectionApply apply);

    @Select("""
            SELECT id, user_id AS userId, attendance_record_id AS attendanceRecordId,
                   correction_type AS correctionType, correction_time AS correctionTime,
                   reason, flow_apply_id AS flowApplyId, status,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM attendance_correction_apply
            WHERE user_id = #{userId}
            ORDER BY created_at DESC
            """)
    List<AttendanceCorrectionApply> listByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT id, user_id AS userId, attendance_record_id AS attendanceRecordId,
                   correction_type AS correctionType, correction_time AS correctionTime,
                   reason, flow_apply_id AS flowApplyId, status,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM attendance_correction_apply
            WHERE flow_apply_id = #{flowApplyId}
            LIMIT 1
            """)
    AttendanceCorrectionApply findByFlowApplyId(@Param("flowApplyId") Long flowApplyId);

    @Update("""
            UPDATE attendance_correction_apply
            SET status = #{status}
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Select("SELECT manager_id FROM sys_user WHERE id = #{userId} AND is_deleted = 0")
    Long selectManagerIdByUserId(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO flow_apply (apply_no, applicant_id, applicant_dept_id, approver_id, apply_type, title, reason, start_time, end_time, duration_hours, status, current_node)
            VALUES (#{applyNo}, #{applicantId}, #{deptId}, #{approverId}, #{applyType}, #{title}, #{reason}, #{startTime}, #{endTime}, #{durationHours}, #{status}, #{currentNode})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertFlowApply(com.officeflow.attendance.dto.FlowApplyInsertParams params);

    @Insert("""
            INSERT INTO flow_approve_record (flow_apply_id, approver_id, action, approved_at)
            VALUES (#{flowApplyId}, #{approverId}, 'SUBMIT', NOW())
            """)
    int insertApproveRecord(@Param("flowApplyId") Long flowApplyId, @Param("approverId") Long approverId);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM attendance_correction_apply
            WHERE user_id = #{userId}
              AND (
                <if test="attendanceRecordId != null">attendance_record_id = #{attendanceRecordId} OR</if>
                DATE(correction_time) = #{workDate}
              )
              AND status IN ('PENDING', 'REJECTED')
            </script>
            """)
    int countActiveOrRejectedCorrection(@Param("userId") Long userId,
                                         @Param("attendanceRecordId") Long attendanceRecordId,
                                         @Param("workDate") java.time.LocalDate workDate);
}

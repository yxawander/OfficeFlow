package com.officeflow.flow.mapper;

import com.officeflow.flow.dto.FlowApplyQueryDTO;
import com.officeflow.flow.entity.FlowApply;
import com.officeflow.flow.vo.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FlowApplyMapper {

    int insert(FlowApply flowApply);

    FlowApply selectById(Long id);

    FlowApply selectByApplyNo(String applyNo);

    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("approvedAt") LocalDateTime approvedAt);

    List<FlowApplyListVO> selectUserApplies(@Param("dto") FlowApplyQueryDTO dto, @Param("userId") Long userId);

    Long countUserApplies(@Param("dto") FlowApplyQueryDTO dto, @Param("userId") Long userId);

    FlowApplyDetailVO selectDetailById(@Param("id") Long id);

    List<FlowPendingVO> selectPendingApplies(@Param("dto") FlowApplyQueryDTO dto, @Param("approverId") Long approverId, @Param("deptId") Long deptId);

    Long countPendingApplies(@Param("dto") FlowApplyQueryDTO dto, @Param("approverId") Long approverId, @Param("deptId") Long deptId);

    List<FlowProcessedVO> selectProcessedApplies(@Param("dto") FlowApplyQueryDTO dto, @Param("approverId") Long approverId, @Param("deptId") Long deptId);

    Long countProcessedApplies(@Param("dto") FlowApplyQueryDTO dto, @Param("approverId") Long approverId, @Param("deptId") Long deptId);

    List<FlowApprovedVO> selectAllApproved(@Param("dto") FlowApplyQueryDTO dto, @Param("deptId") Long deptId);

    Long countAllApproved(@Param("dto") FlowApplyQueryDTO dto, @Param("deptId") Long deptId);

    int update(FlowApply flowApply);

    int deleteById(@Param("id") Long id);

    @org.apache.ibatis.annotations.Update("UPDATE attendance_correction_apply SET status = #{status} WHERE flow_apply_id = #{flowApplyId}")
    int updateCorrectionStatusByFlowApplyId(@Param("flowApplyId") Long flowApplyId, @Param("status") String status);

    @org.apache.ibatis.annotations.Select("""
            SELECT id, user_id AS userId, attendance_record_id AS attendanceRecordId,
                   correction_type AS correctionType, correction_time AS correctionTime
            FROM attendance_correction_apply
            WHERE flow_apply_id = #{flowApplyId}
            LIMIT 1
            """)
    java.util.Map<String, Object> selectCorrectionByFlowApplyId(@Param("flowApplyId") Long flowApplyId);

    @org.apache.ibatis.annotations.Update("""
            UPDATE attendance_record
            SET check_in_time = CASE WHEN #{correctionType} = 'CHECK_IN' THEN #{correctionTime} ELSE check_in_time END,
                check_out_time = CASE WHEN #{correctionType} = 'CHECK_OUT' THEN #{correctionTime} ELSE check_out_time END,
                source = 'MANUAL'
            WHERE (id = #{recordId} AND #{recordId} IS NOT NULL) OR (user_id = #{userId} AND work_date = DATE(#{correctionTime}))
            """)
    int updateAttendanceRecordForCorrection(@Param("recordId") Long recordId,
                                             @Param("userId") Long userId,
                                             @Param("correctionType") String correctionType,
                                             @Param("correctionTime") LocalDateTime correctionTime);

    @org.apache.ibatis.annotations.Insert("""
            INSERT INTO attendance_record (user_id, dept_id, work_date, check_in_time, check_out_time, status, source)
            VALUES (
                #{userId},
                #{deptId},
                DATE(#{correctionTime}),
                CASE WHEN #{correctionType} = 'CHECK_IN' THEN #{correctionTime} ELSE NULL END,
                CASE WHEN #{correctionType} = 'CHECK_OUT' THEN #{correctionTime} ELSE NULL END,
                'MISSING_CARD',
                'MANUAL'
            )
            ON DUPLICATE KEY UPDATE
                check_in_time = CASE WHEN #{correctionType} = 'CHECK_IN' THEN #{correctionTime} ELSE check_in_time END,
                check_out_time = CASE WHEN #{correctionType} = 'CHECK_OUT' THEN #{correctionTime} ELSE check_out_time END,
                source = 'MANUAL'
            """)
    int insertAttendanceRecordForCorrection(@Param("userId") Long userId,
                                            @Param("deptId") Long deptId,
                                            @Param("correctionType") String correctionType,
                                            @Param("correctionTime") LocalDateTime correctionTime);

    @org.apache.ibatis.annotations.Update("""
            UPDATE attendance_record
            SET work_minutes = CASE
                    WHEN check_in_time IS NOT NULL AND check_out_time IS NOT NULL
                    THEN GREATEST(TIMESTAMPDIFF(MINUTE, check_in_time, check_out_time), 0)
                    ELSE 0
                END,
                late_minutes = CASE
                    WHEN check_in_time IS NOT NULL AND TIME(check_in_time) > '09:10:00'
                    THEN TIMESTAMPDIFF(MINUTE, CONCAT(work_date, ' 09:00:00'), check_in_time)
                    ELSE 0
                END,
                early_leave_minutes = CASE
                    WHEN check_out_time IS NOT NULL AND TIME(check_out_time) < '17:50:00'
                    THEN TIMESTAMPDIFF(MINUTE, check_out_time, CONCAT(work_date, ' 18:00:00'))
                    ELSE 0
                END,
                status = CASE
                    WHEN check_in_time IS NULL OR check_out_time IS NULL THEN 'MISSING_CARD'
                    WHEN TIME(check_in_time) > '09:10:00' AND TIME(check_out_time) < '17:50:00' THEN 'LATE_AND_EARLY'
                    WHEN TIME(check_in_time) > '09:10:00' THEN 'LATE'
                    WHEN TIME(check_out_time) < '17:50:00' THEN 'EARLY_LEAVE'
                    ELSE 'RECHECKED'
                END,
                source = 'MANUAL'
            WHERE user_id = #{userId}
              AND work_date = DATE(#{correctionTime})
            """)
    int recalculateAttendanceRecordAfterCorrection(@Param("userId") Long userId,
                                                   @Param("correctionTime") LocalDateTime correctionTime);

    @org.apache.ibatis.annotations.Insert("""
            INSERT INTO attendance_record (user_id, dept_id, work_date, status, source)
            VALUES (#{userId}, #{deptId}, #{workDate}, 'ON_LEAVE', 'MANUAL')
            ON DUPLICATE KEY UPDATE status = 'ON_LEAVE', source = 'MANUAL'
            """)
    int upsertAttendanceRecordForLeave(@Param("userId") Long userId,
                                        @Param("deptId") Long deptId,
                                        @Param("workDate") java.time.LocalDate workDate);
}

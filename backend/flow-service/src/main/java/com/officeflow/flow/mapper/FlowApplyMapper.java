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

    List<FlowApply> selectOverduePendingApplies(@Param("timeoutHours") int timeoutHours);

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

    List<com.officeflow.flow.document.FlowApplyDocument> selectRecentForEs(@Param("minutes") int minutes);

    List<com.officeflow.flow.document.FlowApplyDocument> selectAllForEs();
}

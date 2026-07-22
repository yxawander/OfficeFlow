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

    Long selectManagerIdByUserId(Long userId);

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
}

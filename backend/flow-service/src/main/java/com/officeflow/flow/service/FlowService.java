package com.officeflow.flow.service;

import com.officeflow.common.api.PageResult;
import com.officeflow.flow.dto.*;
import com.officeflow.flow.vo.*;

public interface FlowService {

    // 用户端
    FlowApplyDetailVO createApply(FlowApplyCreateDTO dto, Long applicantId, Long deptId);

    PageResult<FlowApplyListVO> getMyApplies(FlowApplyQueryDTO dto, Long userId);

    FlowApplyDetailVO getApplyDetail(Long id);

    void cancelApply(Long id, Long userId);

    void updateApply(Long id, FlowApplyUpdateDTO dto, Long userId);

    void deleteApply(Long id, Long userId);

    // 审批端
    PageResult<FlowPendingVO> getPendingApplies(FlowApplyQueryDTO dto, Long approverId, Long deptId);

    PageResult<FlowProcessedVO> getProcessedApplies(FlowApplyQueryDTO dto, Long approverId, Long deptId);

    PageResult<FlowApprovedVO> getAllApprovedApplies(FlowApplyQueryDTO dto, Long deptId);

    void approveApply(Long id, FlowApproveDTO dto, Long approverId);

    void rejectApply(Long id, FlowRejectDTO dto, Long approverId);

    // 定时任务
    int autoRejectOverdueApplies();
}

package com.officeflow.flow.controller;

import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.api.PageResult;
import com.officeflow.common.constant.CommonConstants;
import com.officeflow.flow.dto.FlowApplyQueryDTO;
import com.officeflow.flow.dto.FlowApproveDTO;
import com.officeflow.flow.dto.FlowRejectDTO;
import com.officeflow.flow.service.FlowService;
import com.officeflow.flow.vo.FlowApprovedVO;
import com.officeflow.flow.vo.FlowPendingVO;
import com.officeflow.flow.vo.FlowProcessedVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/flow/admin")
@RequiredArgsConstructor
public class FlowAdminController {

    private final FlowService flowService;

    @GetMapping("/applies/pending")
    public ApiResponse<PageResult<FlowPendingVO>> getPendingApplies(FlowApplyQueryDTO dto,
                                                                     HttpServletRequest request) {
        Long approverId = getUserId(request);
        Long deptId = getDeptId(request);
        return ApiResponse.ok(flowService.getPendingApplies(dto, approverId, deptId));
    }

    @GetMapping("/applies/processed")
    public ApiResponse<PageResult<FlowProcessedVO>> getProcessedApplies(FlowApplyQueryDTO dto,
                                                                         HttpServletRequest request) {
        Long approverId = getUserId(request);
        Long deptId = getDeptId(request);
        return ApiResponse.ok(flowService.getProcessedApplies(dto, approverId, deptId));
    }

    @GetMapping("/applies/approved")
    public ApiResponse<PageResult<FlowApprovedVO>> getAllApprovedApplies(FlowApplyQueryDTO dto,
                                                                          HttpServletRequest request) {
        Long deptId = getDeptId(request);
        return ApiResponse.ok(flowService.getAllApprovedApplies(dto, deptId));
    }

    @PostMapping("/applies/{id}/approve")
    public ApiResponse<Void> approveApply(@PathVariable Long id,
                                           @Valid @RequestBody FlowApproveDTO dto,
                                           HttpServletRequest request) {
        Long approverId = getUserId(request);
        flowService.approveApply(id, dto, approverId);
        return ApiResponse.ok();
    }

    @PostMapping("/applies/{id}/reject")
    public ApiResponse<Void> rejectApply(@PathVariable Long id,
                                          @Valid @RequestBody FlowRejectDTO dto,
                                          HttpServletRequest request) {
        Long approverId = getUserId(request);
        flowService.rejectApply(id, dto, approverId);
        return ApiResponse.ok();
    }

    private Long getUserId(HttpServletRequest request) {
        String userIdStr = request.getHeader(CommonConstants.LOGIN_USER_ID_HEADER);
        if (userIdStr == null) {
            throw new com.officeflow.common.exception.BusinessException("用户未登录");
        }
        return Long.parseLong(userIdStr);
    }

    private Long getDeptId(HttpServletRequest request) {
        String deptIdStr = request.getHeader(CommonConstants.LOGIN_DEPT_ID_HEADER);
        if (deptIdStr != null) {
            return Long.parseLong(deptIdStr);
        }
        return null;
    }
}

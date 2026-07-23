package com.officeflow.api.flow.client;

import com.officeflow.api.flow.dto.FlowApplyQueryDTO;
import com.officeflow.api.flow.dto.FlowApproveDTO;
import com.officeflow.api.flow.dto.FlowRejectDTO;
import com.officeflow.api.flow.vo.FlowApprovedVO;
import com.officeflow.api.flow.vo.FlowPendingVO;
import com.officeflow.api.flow.vo.FlowProcessedVO;
import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.api.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "flow-service", path = "/api/flow/admin")
public interface FlowAdminClient {

    @GetMapping("/applies/pending")
    ApiResponse<PageResult<FlowPendingVO>> getPendingApplies(@SpringQueryMap FlowApplyQueryDTO dto,
                                                              @RequestHeader("X-Login-User-Id") Long approverId,
                                                              @RequestHeader(value = "X-Login-Dept-Id", required = false) Long deptId);

    @GetMapping("/applies/processed")
    ApiResponse<PageResult<FlowProcessedVO>> getProcessedApplies(@SpringQueryMap FlowApplyQueryDTO dto,
                                                                   @RequestHeader("X-Login-User-Id") Long approverId,
                                                                   @RequestHeader(value = "X-Login-Dept-Id", required = false) Long deptId);

    @GetMapping("/applies/approved")
    ApiResponse<PageResult<FlowApprovedVO>> getAllApprovedApplies(@SpringQueryMap FlowApplyQueryDTO dto,
                                                                    @RequestHeader(value = "X-Login-Dept-Id", required = false) Long deptId);

    @PostMapping("/applies/{id}/approve")
    ApiResponse<Void> approveApply(@PathVariable Long id,
                                    @RequestBody FlowApproveDTO dto,
                                    @RequestHeader("X-Login-User-Id") Long approverId);

    @PostMapping("/applies/{id}/reject")
    ApiResponse<Void> rejectApply(@PathVariable Long id,
                                   @RequestBody FlowRejectDTO dto,
                                   @RequestHeader("X-Login-User-Id") Long approverId);
}

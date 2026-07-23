package com.officeflow.api.flow.client;

import com.officeflow.api.flow.dto.FlowApplyCreateDTO;
import com.officeflow.api.flow.dto.FlowApplyQueryDTO;
import com.officeflow.api.flow.dto.FlowApplyUpdateDTO;
import com.officeflow.api.flow.vo.FlowApplyDetailVO;
import com.officeflow.api.flow.vo.FlowApplyListVO;
import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.api.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "flow-service",contextId = "flowClient", path = "/api/flow")
public interface FlowClient {

    @PostMapping("/applies")
    ApiResponse<FlowApplyDetailVO> createApply(@RequestBody FlowApplyCreateDTO dto,
                                                @RequestHeader("X-Login-User-Id") Long userId,
                                                @RequestHeader(value = "X-Login-Dept-Id", required = false) Long deptId);

    @GetMapping("/applies/my")
    ApiResponse<PageResult<FlowApplyListVO>> getMyApplies(@SpringQueryMap FlowApplyQueryDTO dto,
                                                           @RequestHeader("X-Login-User-Id") Long userId);

    @GetMapping("/applies/{id}")
    ApiResponse<FlowApplyDetailVO> getApplyDetail(@PathVariable Long id);

    @PutMapping("/applies/{id}")
    ApiResponse<Void> updateApply(@PathVariable Long id,
                                   @RequestBody FlowApplyUpdateDTO dto,
                                   @RequestHeader("X-Login-User-Id") Long userId);

    @DeleteMapping("/applies/{id}")
    ApiResponse<Void> deleteApply(@PathVariable Long id,
                                   @RequestHeader("X-Login-User-Id") Long userId);

    @PutMapping("/applies/{id}/cancel")
    ApiResponse<Void> cancelApply(@PathVariable Long id,
                                   @RequestHeader("X-Login-User-Id") Long userId);
}

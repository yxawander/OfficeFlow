package com.officeflow.flow.controller;

import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.api.PageResult;
import com.officeflow.common.constant.CommonConstants;
import com.officeflow.flow.dto.FlowApplyCreateDTO;
import com.officeflow.flow.dto.FlowApplyQueryDTO;
import com.officeflow.flow.dto.FlowApplyUpdateDTO;
import com.officeflow.flow.service.FlowService;
import com.officeflow.flow.vo.FlowApplyDetailVO;
import com.officeflow.flow.vo.FlowApplyListVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/flow")
@RequiredArgsConstructor
public class FlowController {

    private final FlowService flowService;

    @PostMapping("/applies")
    public ApiResponse<FlowApplyDetailVO> createApply(@Valid @RequestBody FlowApplyCreateDTO dto,
                                                       HttpServletRequest request) {
        Long userId = getUserId(request);
        Long deptId = getDeptId(request);
        return ApiResponse.ok(flowService.createApply(dto, userId, deptId));
    }

    @GetMapping("/applies/my")
    public ApiResponse<PageResult<FlowApplyListVO>> getMyApplies(FlowApplyQueryDTO dto,
                                                                  HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.ok(flowService.getMyApplies(dto, userId));
    }

    @GetMapping("/applies/{id}")
    public ApiResponse<FlowApplyDetailVO> getApplyDetail(@PathVariable Long id) {
        return ApiResponse.ok(flowService.getApplyDetail(id));
    }

    @PutMapping("/applies/{id}")
    public ApiResponse<Void> updateApply(@PathVariable Long id,
                                          @Valid @RequestBody FlowApplyUpdateDTO dto,
                                          HttpServletRequest request) {
        Long userId = getUserId(request);
        flowService.updateApply(id, dto, userId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/applies/{id}")
    public ApiResponse<Void> deleteApply(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        flowService.deleteApply(id, userId);
        return ApiResponse.ok();
    }

    @PutMapping("/applies/{id}/cancel")
    public ApiResponse<Void> cancelApply(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        flowService.cancelApply(id, userId);
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

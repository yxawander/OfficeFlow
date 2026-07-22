package com.officeflow.flow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.officeflow.common.api.PageResult;
import com.officeflow.common.exception.GlobalExceptionHandler;
import com.officeflow.flow.dto.FlowApproveDTO;
import com.officeflow.flow.dto.FlowRejectDTO;
import com.officeflow.flow.service.FlowService;
import com.officeflow.flow.vo.FlowApprovedVO;
import com.officeflow.flow.vo.FlowPendingVO;
import com.officeflow.flow.vo.FlowProcessedVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("审批管理控制器单元测试")
class FlowAdminControllerTest {

    @Mock
    private FlowService flowService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FlowAdminController(flowService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("待审批列表 - 成功")
    void getPendingApplies_Success() throws Exception {
        PageResult<FlowPendingVO> pageResult = PageResult.of(1, 1, 10, List.of(new FlowPendingVO()));
        when(flowService.getPendingApplies(any(), any(), any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/flow/admin/applies/pending")
                        .header("X-Login-User-Id", "200")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("待审批列表 - 用户未登录")
    void getPendingApplies_NotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/flow/admin/applies/pending")
                        .param("pageNum", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(com.officeflow.common.api.ResultCode.FAIL.code()));
    }

    @Test
    @DisplayName("已审批列表 - 成功")
    void getProcessedApplies_Success() throws Exception {
        PageResult<FlowProcessedVO> pageResult = PageResult.of(1, 1, 10, List.of(new FlowProcessedVO()));
        when(flowService.getProcessedApplies(any(), any(), any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/flow/admin/applies/processed")
                        .header("X-Login-User-Id", "200")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("审批通过 - 成功")
    void approveApply_Success() throws Exception {
        FlowApproveDTO dto = new FlowApproveDTO();
        dto.setComment("同意");

        mockMvc.perform(post("/api/flow/admin/applies/1/approve")
                        .header("X-Login-User-Id", "200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("审批通过 - 用户未登录")
    void approveApply_NotLoggedIn() throws Exception {
        mockMvc.perform(post("/api/flow/admin/applies/1/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(com.officeflow.common.api.ResultCode.FAIL.code()));
    }

    @Test
    @DisplayName("审批驳回 - 成功")
    void rejectApply_Success() throws Exception {
        FlowRejectDTO dto = new FlowRejectDTO();
        dto.setComment("申请理由不充分");

        mockMvc.perform(post("/api/flow/admin/applies/1/reject")
                        .header("X-Login-User-Id", "200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("审批驳回 - 验证失败 - 意见为空")
    void rejectApply_ValidationError() throws Exception {
        FlowRejectDTO dto = new FlowRejectDTO();
        dto.setComment("");

        mockMvc.perform(post("/api/flow/admin/applies/1/reject")
                        .header("X-Login-User-Id", "200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(com.officeflow.common.api.ResultCode.PARAM_ERROR.code()));
    }

    @Test
    @DisplayName("待审批列表 - 按类型筛选")
    void getPendingApplies_WithFilter() throws Exception {
        PageResult<FlowPendingVO> pageResult = PageResult.of(0, 1, 10, List.of());
        when(flowService.getPendingApplies(any(), any(), any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/flow/admin/applies/pending")
                        .header("X-Login-User-Id", "200")
                        .param("pageNum", "1")
                        .param("applyType", "LEAVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("所有已审批申请 - 成功")
    void getAllApprovedApplies_Success() throws Exception {
        PageResult<FlowApprovedVO> pageResult = PageResult.of(1, 1, 10, List.of(new FlowApprovedVO()));
        when(flowService.getAllApprovedApplies(any(), any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/flow/admin/applies/approved")
                        .header("X-Login-User-Id", "200")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }
}

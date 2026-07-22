package com.officeflow.flow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.officeflow.common.api.PageResult;
import com.officeflow.common.exception.GlobalExceptionHandler;
import com.officeflow.flow.dto.FlowApplyCreateDTO;
import com.officeflow.flow.service.FlowService;
import com.officeflow.flow.vo.FlowApplyDetailVO;
import com.officeflow.flow.vo.FlowApplyListVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("审批控制器单元测试")
class FlowControllerTest {

    @Mock
    private FlowService flowService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FlowController(flowService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("提交申请 - 成功")
    void createApply_Success() throws Exception {
        FlowApplyCreateDTO dto = buildCreateDTO();
        FlowApplyDetailVO detailVO = new FlowApplyDetailVO();
        detailVO.setId(1L);
        detailVO.setTitle("年假申请");

        when(flowService.createApply(any(), any(), any())).thenReturn(detailVO);

        mockMvc.perform(post("/api/flow/applies")
                        .header("X-Login-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("提交申请 - 用户未登录")
    void createApply_NotLoggedIn() throws Exception {
        mockMvc.perform(post("/api/flow/applies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(com.officeflow.common.api.ResultCode.FAIL.code()));
    }

    @Test
    @DisplayName("提交申请 - 验证失败")
    void createApply_ValidationError() throws Exception {
        FlowApplyCreateDTO dto = new FlowApplyCreateDTO();
        dto.setApplyType("");
        dto.setTitle("");

        mockMvc.perform(post("/api/flow/applies")
                        .header("X-Login-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(com.officeflow.common.api.ResultCode.PARAM_ERROR.code()));
    }

    @Test
    @DisplayName("我的申请列表 - 成功")
    void getMyApplies_Success() throws Exception {
        PageResult<FlowApplyListVO> pageResult = PageResult.of(1, 1, 10, List.of(new FlowApplyListVO()));
        when(flowService.getMyApplies(any(), any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/flow/applies/my")
                        .header("X-Login-User-Id", "100")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("申请详情 - 成功")
    void getApplyDetail_Success() throws Exception {
        FlowApplyDetailVO detailVO = new FlowApplyDetailVO();
        detailVO.setId(1L);
        detailVO.setTitle("年假申请");

        when(flowService.getApplyDetail(1L)).thenReturn(detailVO);

        mockMvc.perform(get("/api/flow/applies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("撤销申请 - 成功")
    void cancelApply_Success() throws Exception {
        mockMvc.perform(put("/api/flow/applies/1/cancel")
                        .header("X-Login-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("编辑申请 - 成功")
    void updateApply_Success() throws Exception {
        mockMvc.perform(put("/api/flow/applies/1")
                        .header("X-Login-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("删除申请 - 成功")
    void deleteApply_Success() throws Exception {
        mockMvc.perform(delete("/api/flow/applies/1")
                        .header("X-Login-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("撤销申请 - 用户未登录")
    void cancelApply_NotLoggedIn() throws Exception {
        mockMvc.perform(put("/api/flow/applies/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(com.officeflow.common.api.ResultCode.FAIL.code()));
    }

    private FlowApplyCreateDTO buildCreateDTO() {
        FlowApplyCreateDTO dto = new FlowApplyCreateDTO();
        dto.setApplyType("LEAVE");
        dto.setTitle("年假申请");
        dto.setReason("个人原因休假");
        dto.setStartTime(LocalDateTime.now().plusDays(1));
        dto.setEndTime(LocalDateTime.now().plusDays(3));
        dto.setDurationHours(new BigDecimal("16"));
        return dto;
    }
}

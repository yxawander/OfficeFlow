package com.officeflow.flow.service.impl;

import com.officeflow.common.api.PageResult;
import com.officeflow.common.exception.BusinessException;
import com.officeflow.flow.dto.*;
import com.officeflow.flow.entity.FlowApply;
import com.officeflow.flow.mapper.FlowApplyMapper;
import com.officeflow.flow.mapper.FlowApproveRecordMapper;
import com.officeflow.flow.mapper.FlowCcMapper;
import com.officeflow.flow.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("审批服务单元测试")
class FlowServiceImplTest {

    @Mock
    private FlowApplyMapper flowApplyMapper;

    @Mock
    private FlowApproveRecordMapper flowApproveRecordMapper;

    @Mock
    private FlowCcMapper flowCcMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private FlowServiceImpl flowService;

    private FlowApplyCreateDTO createDTO;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        createDTO = new FlowApplyCreateDTO();
        createDTO.setApplyType("LEAVE");
        createDTO.setTitle("年假申请");
        createDTO.setReason("个人原因休假");
        createDTO.setStartTime(LocalDateTime.now().plusDays(1));
        createDTO.setEndTime(LocalDateTime.now().plusDays(3));
        createDTO.setDurationHours(new BigDecimal("16"));
        createDTO.setApproverId(200L);
    }

    @Test
    @DisplayName("提交请假申请 - 成功")
    void createApply_Success() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(flowApplyMapper.selectManagerIdByUserId(100L)).thenReturn(200L);
        when(flowApplyMapper.insert(any(FlowApply.class))).thenAnswer(invocation -> {
            FlowApply apply = invocation.getArgument(0);
            apply.setId(1L);
            return 1;
        });
        when(flowApproveRecordMapper.insert(any())).thenReturn(1);

        FlowApplyDetailVO detailVO = new FlowApplyDetailVO();
        detailVO.setId(1L);
        detailVO.setTitle("年假申请");
        when(flowApplyMapper.selectDetailById(1L)).thenReturn(detailVO);

        FlowApplyDetailVO result = flowService.createApply(createDTO, 100L, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(flowApplyMapper).insert(any(FlowApply.class));
        verify(flowApproveRecordMapper).insert(any());
        verify(flowCcMapper, never()).batchInsert(any());
    }

    @Test
    @DisplayName("提交申请 - 含抄送人")
    void createApply_WithCc() {
        createDTO.setCcUserIds(List.of(300L, 301L));

        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(flowApplyMapper.selectManagerIdByUserId(100L)).thenReturn(200L);
        when(flowApplyMapper.insert(any(FlowApply.class))).thenAnswer(invocation -> {
            FlowApply apply = invocation.getArgument(0);
            apply.setId(1L);
            return 1;
        });
        when(flowApproveRecordMapper.insert(any())).thenReturn(1);
        when(flowCcMapper.batchInsert(any())).thenReturn(2);

        FlowApplyDetailVO detailVO = new FlowApplyDetailVO();
        detailVO.setId(1L);
        when(flowApplyMapper.selectDetailById(1L)).thenReturn(detailVO);

        FlowApplyDetailVO result = flowService.createApply(createDTO, 100L, 1L);

        assertThat(result).isNotNull();
        verify(flowCcMapper).batchInsert(any());
    }

    @Test
    @DisplayName("获取我的申请列表 - 成功")
    void getMyApplies_Success() {
        FlowApplyQueryDTO dto = new FlowApplyQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);

        when(flowApplyMapper.selectUserApplies(any(), eq(100L))).thenReturn(List.of(new FlowApplyListVO()));
        when(flowApplyMapper.countUserApplies(any(), eq(100L))).thenReturn(1L);

        PageResult<FlowApplyListVO> result = flowService.getMyApplies(dto, 100L);

        assertThat(result).isNotNull();
        assertThat(result.records()).hasSize(1);
        assertThat(result.total()).isEqualTo(1);
    }

    @Test
    @DisplayName("获取申请详情 - 成功")
    void getApplyDetail_Success() {
        FlowApplyDetailVO detailVO = new FlowApplyDetailVO();
        detailVO.setId(1L);
        detailVO.setTitle("年假申请");

        when(flowApplyMapper.selectDetailById(1L)).thenReturn(detailVO);
        when(flowApproveRecordMapper.selectByApplyId(1L))
                .thenReturn(List.of(new FlowApproveRecordVO()));

        FlowApplyDetailVO result = flowService.getApplyDetail(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getApproveRecords()).hasSize(1);
    }

    @Test
    @DisplayName("获取申请详情 - 申请不存在")
    void getApplyDetail_NotExists() {
        when(flowApplyMapper.selectDetailById(999L)).thenReturn(null);

        assertThatThrownBy(() -> flowService.getApplyDetail(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("审批申请不存在");
    }

    @Test
    @DisplayName("撤销申请 - 成功")
    void cancelApply_Success() {
        FlowApply apply = new FlowApply();
        apply.setId(1L);
        apply.setApplicantId(100L);
        apply.setStatus("PENDING");

        when(flowApplyMapper.selectById(1L)).thenReturn(apply);
        when(flowApplyMapper.updateStatus(1L, "CANCELED", null)).thenReturn(1);
        when(flowApproveRecordMapper.insert(any())).thenReturn(1);

        flowService.cancelApply(1L, 100L);

        verify(flowApplyMapper).updateStatus(1L, "CANCELED", null);
        verify(flowApproveRecordMapper).insert(any());
    }

    @Test
    @DisplayName("撤销申请 - 不是申请人")
    void cancelApply_NotOwner() {
        FlowApply apply = new FlowApply();
        apply.setId(1L);
        apply.setApplicantId(999L);
        apply.setStatus("PENDING");

        when(flowApplyMapper.selectById(1L)).thenReturn(apply);

        assertThatThrownBy(() -> flowService.cancelApply(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("仅申请人可撤销自己的申请");
    }

    @Test
    @DisplayName("撤销申请 - 状态不允许")
    void cancelApply_StatusNotAllowed() {
        FlowApply apply = new FlowApply();
        apply.setId(1L);
        apply.setApplicantId(100L);
        apply.setStatus("APPROVED");

        when(flowApplyMapper.selectById(1L)).thenReturn(apply);

        assertThatThrownBy(() -> flowService.cancelApply(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("仅待审批状态可撤销");
    }

    @Test
    @DisplayName("获取待审批列表 - 成功")
    void getPendingApplies_Success() {
        FlowApplyQueryDTO dto = new FlowApplyQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);

        when(flowApplyMapper.selectPendingApplies(any(), eq(200L), any())).thenReturn(List.of(new FlowPendingVO()));
        when(flowApplyMapper.countPendingApplies(any(), eq(200L), any())).thenReturn(1L);

        PageResult<FlowPendingVO> result = flowService.getPendingApplies(dto, 200L, 1L);

        assertThat(result).isNotNull();
        assertThat(result.records()).hasSize(1);
        assertThat(result.total()).isEqualTo(1);
    }

    @Test
    @DisplayName("获取已审批列表 - 成功")
    void getProcessedApplies_Success() {
        FlowApplyQueryDTO dto = new FlowApplyQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);

        when(flowApplyMapper.selectProcessedApplies(any(), eq(200L), any())).thenReturn(List.of(new FlowProcessedVO()));
        when(flowApplyMapper.countProcessedApplies(any(), eq(200L), any())).thenReturn(1L);

        PageResult<FlowProcessedVO> result = flowService.getProcessedApplies(dto, 200L, 1L);

        assertThat(result).isNotNull();
        assertThat(result.records()).hasSize(1);
        assertThat(result.total()).isEqualTo(1);
    }

    @Test
    @DisplayName("审批通过 - 成功")
    void approveApply_Success() {
        FlowApply apply = new FlowApply();
        apply.setId(1L);
        apply.setStatus("PENDING");
        apply.setApproverId(200L);

        when(flowApplyMapper.selectById(1L)).thenReturn(apply);
        when(flowApplyMapper.updateStatus(eq(1L), eq("APPROVED"), any())).thenReturn(1);
        when(flowApproveRecordMapper.insert(any())).thenReturn(1);

        FlowApproveDTO dto = new FlowApproveDTO();
        dto.setComment("同意");

        flowService.approveApply(1L, dto, 200L);

        verify(flowApplyMapper).updateStatus(eq(1L), eq("APPROVED"), any());
        verify(flowApproveRecordMapper).insert(any());
    }

    @Test
    @DisplayName("审批通过 - 不是审批人")
    void approveApply_NotApprover() {
        FlowApply apply = new FlowApply();
        apply.setId(1L);
        apply.setStatus("PENDING");
        apply.setApproverId(999L);

        when(flowApplyMapper.selectById(1L)).thenReturn(apply);

        assertThatThrownBy(() -> flowService.approveApply(1L, new FlowApproveDTO(), 200L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("您不是该申请的审批人");
    }

    @Test
    @DisplayName("审批通过 - 申请已被处理")
    void approveApply_AlreadyProcessed() {
        FlowApply apply = new FlowApply();
        apply.setId(1L);
        apply.setStatus("APPROVED");
        apply.setApproverId(200L);

        when(flowApplyMapper.selectById(1L)).thenReturn(apply);

        assertThatThrownBy(() -> flowService.approveApply(1L, new FlowApproveDTO(), 200L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该申请已被处理");
    }

    @Test
    @DisplayName("驳回申请 - 成功")
    void rejectApply_Success() {
        FlowApply apply = new FlowApply();
        apply.setId(1L);
        apply.setStatus("PENDING");
        apply.setApproverId(200L);

        when(flowApplyMapper.selectById(1L)).thenReturn(apply);
        when(flowApplyMapper.updateStatus(eq(1L), eq("REJECTED"), isNull())).thenReturn(1);
        when(flowApproveRecordMapper.insert(any())).thenReturn(1);

        FlowRejectDTO dto = new FlowRejectDTO();
        dto.setComment("申请理由不充分");

        flowService.rejectApply(1L, dto, 200L);

        verify(flowApplyMapper).updateStatus(eq(1L), eq("REJECTED"), isNull());
        verify(flowApproveRecordMapper).insert(any());
    }

    @Test
    @DisplayName("驳回申请 - 不是审批人")
    void rejectApply_NotApprover() {
        FlowApply apply = new FlowApply();
        apply.setId(1L);
        apply.setStatus("PENDING");
        apply.setApproverId(999L);

        when(flowApplyMapper.selectById(1L)).thenReturn(apply);

        FlowRejectDTO dto = new FlowRejectDTO();
        dto.setComment("申请理由不充分");

        assertThatThrownBy(() -> flowService.rejectApply(1L, dto, 200L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("您不是该申请的审批人");
    }

    @Test
    @DisplayName("提交加班申请 - 成功")
    void createApply_Overtime() {
        createDTO.setApplyType("OVERTIME");
        createDTO.setTitle("周末加班申请");

        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(flowApplyMapper.selectManagerIdByUserId(100L)).thenReturn(200L);
        when(flowApplyMapper.insert(any(FlowApply.class))).thenAnswer(invocation -> {
            FlowApply apply = invocation.getArgument(0);
            apply.setId(2L);
            return 1;
        });
        when(flowApproveRecordMapper.insert(any())).thenReturn(1);

        FlowApplyDetailVO detailVO = new FlowApplyDetailVO();
        detailVO.setId(2L);
        detailVO.setApplyType("OVERTIME");
        when(flowApplyMapper.selectDetailById(2L)).thenReturn(detailVO);

        FlowApplyDetailVO result = flowService.createApply(createDTO, 100L, 1L);

        assertThat(result.getApplyType()).isEqualTo("OVERTIME");
    }

    @Test
    @DisplayName("补卡申请 - 成功")
    void createApply_Correction() {
        createDTO.setApplyType("CORRECTION");
        createDTO.setTitle("补卡申请");

        when(valueOperations.increment(anyString())).thenReturn(2L);
        when(flowApplyMapper.selectManagerIdByUserId(100L)).thenReturn(200L);
        when(flowApplyMapper.insert(any(FlowApply.class))).thenAnswer(invocation -> {
            FlowApply apply = invocation.getArgument(0);
            apply.setId(3L);
            return 1;
        });
        when(flowApproveRecordMapper.insert(any())).thenReturn(1);

        FlowApplyDetailVO detailVO = new FlowApplyDetailVO();
        detailVO.setId(3L);
        detailVO.setApplyType("CORRECTION");
        when(flowApplyMapper.selectDetailById(3L)).thenReturn(detailVO);

        FlowApplyDetailVO result = flowService.createApply(createDTO, 100L, 1L);

        assertThat(result.getApplyType()).isEqualTo("CORRECTION");
    }

    @Test
    @DisplayName("编辑申请 - 成功")
    void updateApply_Success() {
        FlowApply apply = new FlowApply();
        apply.setId(1L);
        apply.setApplicantId(100L);
        apply.setStatus("PENDING");

        FlowApplyUpdateDTO dto = new FlowApplyUpdateDTO();
        dto.setTitle("修改后的标题");
        dto.setReason("修改后的原因");
        dto.setStartTime(LocalDateTime.now().plusDays(2));
        dto.setEndTime(LocalDateTime.now().plusDays(4));
        dto.setDurationHours(new BigDecimal("24"));

        when(flowApplyMapper.selectById(1L)).thenReturn(apply);
        when(flowApplyMapper.update(any())).thenReturn(1);

        flowService.updateApply(1L, dto, 100L);

        verify(flowApplyMapper).update(any());
    }

    @Test
    @DisplayName("编辑申请 - 不是申请人")
    void updateApply_NotOwner() {
        FlowApply apply = new FlowApply();
        apply.setId(1L);
        apply.setApplicantId(999L);
        apply.setStatus("PENDING");

        when(flowApplyMapper.selectById(1L)).thenReturn(apply);

        assertThatThrownBy(() -> flowService.updateApply(1L, new FlowApplyUpdateDTO(), 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("仅申请人可编辑自己的申请");
    }

    @Test
    @DisplayName("编辑申请 - 状态不允许")
    void updateApply_StatusNotAllowed() {
        FlowApply apply = new FlowApply();
        apply.setId(1L);
        apply.setApplicantId(100L);
        apply.setStatus("APPROVED");

        when(flowApplyMapper.selectById(1L)).thenReturn(apply);

        assertThatThrownBy(() -> flowService.updateApply(1L, new FlowApplyUpdateDTO(), 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("仅待审批状态可编辑");
    }

    @Test
    @DisplayName("删除申请 - 成功")
    void deleteApply_Success() {
        FlowApply apply = new FlowApply();
        apply.setId(1L);
        apply.setApplicantId(100L);

        when(flowApplyMapper.selectById(1L)).thenReturn(apply);
        when(flowApplyMapper.deleteById(1L)).thenReturn(1);

        flowService.deleteApply(1L, 100L);

        verify(flowApplyMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除申请 - 不是申请人")
    void deleteApply_NotOwner() {
        FlowApply apply = new FlowApply();
        apply.setId(1L);
        apply.setApplicantId(999L);

        when(flowApplyMapper.selectById(1L)).thenReturn(apply);

        assertThatThrownBy(() -> flowService.deleteApply(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("仅申请人可删除自己的申请");
    }

    @Test
    @DisplayName("获取所有已审批申请 - 成功")
    void getAllApprovedApplies_Success() {
        FlowApplyQueryDTO dto = new FlowApplyQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);

        when(flowApplyMapper.selectAllApproved(any(), eq(1L))).thenReturn(List.of(new FlowApprovedVO()));
        when(flowApplyMapper.countAllApproved(any(), eq(1L))).thenReturn(1L);

        PageResult<FlowApprovedVO> result = flowService.getAllApprovedApplies(dto, 1L);

        assertThat(result).isNotNull();
        assertThat(result.records()).hasSize(1);
        assertThat(result.total()).isEqualTo(1);
    }
}

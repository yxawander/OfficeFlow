package com.officeflow.flow.service.impl;

import com.officeflow.common.api.PageResult;
import com.officeflow.common.exception.BusinessException;
import com.officeflow.flow.dto.*;
import com.officeflow.flow.entity.FlowApply;
import com.officeflow.flow.entity.FlowApproveRecord;
import com.officeflow.flow.entity.FlowCc;
import com.officeflow.flow.mapper.FlowApplyMapper;
import com.officeflow.flow.mapper.FlowApproveRecordMapper;
import com.officeflow.flow.mapper.FlowCcMapper;
import com.officeflow.flow.service.FlowService;
import com.officeflow.flow.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowServiceImpl implements FlowService {

    private final FlowApplyMapper flowApplyMapper;
    private final FlowApproveRecordMapper flowApproveRecordMapper;
    private final FlowCcMapper flowCcMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional
    public FlowApplyDetailVO createApply(FlowApplyCreateDTO dto, Long applicantId, Long deptId) {
        validateApplyRequest(dto.getApplyType(), dto.getStartTime(), dto.getEndTime(), dto.getDurationHours());

        FlowApply apply = new FlowApply();
        apply.setApplyNo(generateApplyNo());
        apply.setApplicantId(applicantId);
        apply.setApplicantDeptId(deptId);

        Long managerId = flowApplyMapper.selectManagerIdByUserId(applicantId);
        if (managerId == null) {
            throw new BusinessException("未找到直属领导，无法提交申请");
        }
        apply.setApproverId(managerId);
        apply.setApplyType(dto.getApplyType());
        apply.setTitle(dto.getTitle());
        apply.setReason(dto.getReason());
        apply.setStartTime(dto.getStartTime());
        apply.setEndTime(dto.getEndTime());
        apply.setDurationHours(dto.getDurationHours());
        apply.setStatus("PENDING");
        apply.setCurrentNode("DIRECT_MANAGER");
        apply.setIsDeleted((byte) 0);

        flowApplyMapper.insert(apply);

        FlowApproveRecord record = new FlowApproveRecord();
        record.setFlowApplyId(apply.getId());
        record.setApproverId(applicantId);
        record.setAction("SUBMIT");
        record.setApprovedAt(LocalDateTime.now());
        flowApproveRecordMapper.insert(record);

        if (!CollectionUtils.isEmpty(dto.getCcUserIds())) {
            List<FlowCc> ccList = new ArrayList<>();
            for (Long ccUserId : dto.getCcUserIds()) {
                FlowCc cc = new FlowCc();
                cc.setFlowApplyId(apply.getId());
                cc.setUserId(ccUserId);
                cc.setReadStatus((byte) 0);
                cc.setCreatedAt(LocalDateTime.now());
                cc.setUpdatedAt(LocalDateTime.now());
                ccList.add(cc);
            }
            flowCcMapper.batchInsert(ccList);
        }

        return flowApplyMapper.selectDetailById(apply.getId());
    }

    @Override
    public PageResult<FlowApplyListVO> getMyApplies(FlowApplyQueryDTO dto, Long userId) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        dto.setOffset(offset);

        List<FlowApplyListVO> records = flowApplyMapper.selectUserApplies(dto, userId);
        Long total = flowApplyMapper.countUserApplies(dto, userId);

        return PageResult.of(total, dto.getPageNum(), dto.getPageSize(), records);
    }

    @Override
    public FlowApplyDetailVO getApplyDetail(Long id) {
        FlowApplyDetailVO detail = flowApplyMapper.selectDetailById(id);
        if (detail == null) {
            throw new BusinessException("审批申请不存在");
        }

        List<FlowApproveRecordVO> approveRecords = flowApproveRecordMapper.selectByApplyId(id);
        detail.setApproveRecords(approveRecords);

        return detail;
    }

    @Override
    @Transactional
    public void cancelApply(Long id, Long userId) {
        FlowApply apply = flowApplyMapper.selectById(id);
        if (apply == null) {
            throw new BusinessException("审批申请不存在");
        }
        if (!"PENDING".equals(apply.getStatus())) {
            throw new BusinessException("仅待审批状态可撤销");
        }
        if (!apply.getApplicantId().equals(userId)) {
            throw new BusinessException("仅申请人可撤销自己的申请");
        }

        flowApplyMapper.updateStatus(id, "CANCELED", null);

        FlowApproveRecord record = new FlowApproveRecord();
        record.setFlowApplyId(id);
        record.setApproverId(userId);
        record.setAction("CANCEL");
        record.setComment("申请人撤销");
        record.setApprovedAt(LocalDateTime.now());
        flowApproveRecordMapper.insert(record);
    }

    @Override
    public PageResult<FlowPendingVO> getPendingApplies(FlowApplyQueryDTO dto, Long approverId, Long deptId) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        dto.setOffset(offset);

        List<FlowPendingVO> records = flowApplyMapper.selectPendingApplies(dto, approverId, deptId);
        Long total = flowApplyMapper.countPendingApplies(dto, approverId, deptId);

        return PageResult.of(total, dto.getPageNum(), dto.getPageSize(), records);
    }

    @Override
    public PageResult<FlowProcessedVO> getProcessedApplies(FlowApplyQueryDTO dto, Long approverId, Long deptId) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        dto.setOffset(offset);

        List<FlowProcessedVO> records = flowApplyMapper.selectProcessedApplies(dto, approverId, deptId);
        Long total = flowApplyMapper.countProcessedApplies(dto, approverId, deptId);

        return PageResult.of(total, dto.getPageNum(), dto.getPageSize(), records);
    }

    @Override
    @Transactional
    public void updateApply(Long id, FlowApplyUpdateDTO dto, Long userId) {
        FlowApply apply = flowApplyMapper.selectById(id);
        if (apply == null) {
            throw new BusinessException("审批申请不存在");
        }
        if (!"PENDING".equals(apply.getStatus())) {
            throw new BusinessException("仅待审批状态可编辑");
        }
        if (!apply.getApplicantId().equals(userId)) {
            throw new BusinessException("仅申请人可编辑自己的申请");
        }
        validateApplyRequest(apply.getApplyType(), dto.getStartTime(), dto.getEndTime(), dto.getDurationHours());

        apply.setTitle(dto.getTitle());
        apply.setReason(dto.getReason());
        apply.setStartTime(dto.getStartTime());
        apply.setEndTime(dto.getEndTime());
        apply.setDurationHours(dto.getDurationHours());
        flowApplyMapper.update(apply);
    }

    @Override
    @Transactional
    public void deleteApply(Long id, Long userId) {
        FlowApply apply = flowApplyMapper.selectById(id);
        if (apply == null) {
            throw new BusinessException("审批申请不存在");
        }
        if (!apply.getApplicantId().equals(userId)) {
            throw new BusinessException("仅申请人可删除自己的申请");
        }

        flowApplyMapper.deleteById(id);
    }

    @Override
    public PageResult<FlowApprovedVO> getAllApprovedApplies(FlowApplyQueryDTO dto, Long deptId) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        dto.setOffset(offset);

        List<FlowApprovedVO> records = flowApplyMapper.selectAllApproved(dto, deptId);
        Long total = flowApplyMapper.countAllApproved(dto, deptId);

        return PageResult.of(total, dto.getPageNum(), dto.getPageSize(), records);
    }

    @Override
    @Transactional
    public void approveApply(Long id, FlowApproveDTO dto, Long approverId) {
        FlowApply apply = flowApplyMapper.selectById(id);
        if (apply == null) {
            throw new BusinessException("审批申请不存在");
        }
        if (!"PENDING".equals(apply.getStatus())) {
            throw new BusinessException("该申请已被处理");
        }
        if (!apply.getApproverId().equals(approverId)) {
            throw new BusinessException("您不是该申请的审批人");
        }

        LocalDateTime now = LocalDateTime.now();
        flowApplyMapper.updateStatus(id, "APPROVED", now);

        FlowApproveRecord record = new FlowApproveRecord();
        record.setFlowApplyId(id);
        record.setApproverId(approverId);
        record.setAction("APPROVE");
        record.setComment(dto.getComment());
        record.setApprovedAt(now);
        flowApproveRecordMapper.insert(record);

        // 如果是补卡类型申请，自动同步更新 attendance_correction_apply 与 attendance_record 考勤表
        if ("CORRECTION".equalsIgnoreCase(apply.getApplyType())) {
            flowApplyMapper.updateCorrectionStatusByFlowApplyId(id, "APPROVED");
            java.util.Map<String, Object> corr = flowApplyMapper.selectCorrectionByFlowApplyId(id);
            if (corr != null) {
                Long recordId = corr.get("attendanceRecordId") != null ? Long.parseLong(corr.get("attendanceRecordId").toString()) : null;
                Long userId = corr.get("userId") != null ? Long.parseLong(corr.get("userId").toString()) : apply.getApplicantId();
                String corrType = String.valueOf(corr.get("correctionType"));
                LocalDateTime corrTime = (LocalDateTime) corr.get("correctionTime");
                int updated = flowApplyMapper.updateAttendanceRecordForCorrection(recordId, userId, corrType, corrTime);
                if (updated == 0) {
                    flowApplyMapper.insertAttendanceRecordForCorrection(userId, apply.getApplicantDeptId(), corrType, corrTime);
                }
                flowApplyMapper.recalculateAttendanceRecordAfterCorrection(userId, corrTime);
            }
        } else if ("LEAVE".equalsIgnoreCase(apply.getApplyType())) {
            if (apply.getStartTime() != null && apply.getEndTime() != null) {
                java.time.LocalDate cur = apply.getStartTime().toLocalDate();
                java.time.LocalDate end = apply.getEndTime().toLocalDate();
                while (!cur.isAfter(end)) {
                    flowApplyMapper.upsertAttendanceRecordForLeave(apply.getApplicantId(), apply.getApplicantDeptId(), cur);
                    cur = cur.plusDays(1);
                }
            }
        }
    }

    @Override
    @Transactional
    public void rejectApply(Long id, FlowRejectDTO dto, Long approverId) {
        FlowApply apply = flowApplyMapper.selectById(id);
        if (apply == null) {
            throw new BusinessException("审批申请不存在");
        }
        if (!"PENDING".equals(apply.getStatus())) {
            throw new BusinessException("该申请已被处理");
        }
        if (!apply.getApproverId().equals(approverId)) {
            throw new BusinessException("您不是该申请的审批人");
        }

        flowApplyMapper.updateStatus(id, "REJECTED", null);

        FlowApproveRecord record = new FlowApproveRecord();
        record.setFlowApplyId(id);
        record.setApproverId(approverId);
        record.setAction("REJECT");
        record.setComment(dto.getComment());
        record.setApprovedAt(LocalDateTime.now());
        flowApproveRecordMapper.insert(record);

        if ("CORRECTION".equalsIgnoreCase(apply.getApplyType())) {
            flowApplyMapper.updateCorrectionStatusByFlowApplyId(id, "REJECTED");
        }
    }

    private String generateApplyNo() {
        String today = LocalDate.now().format(DATE_FORMAT);
        String key = "flow:apply:seq:" + today;
        Long seq = stringRedisTemplate.opsForValue().increment(key);
        return "FL" + today + String.format("%06d", seq != null ? seq : 1);
    }

    private void validateApplyRequest(String applyType, LocalDateTime startTime, LocalDateTime endTime, BigDecimal durationHours) {
        if (!"LEAVE".equalsIgnoreCase(applyType) && !"OVERTIME".equalsIgnoreCase(applyType)) {
            throw new BusinessException("仅支持请假和加班申请，补卡申请请在考勤记录中发起");
        }
        if (startTime == null || endTime == null) {
            throw new BusinessException("申请开始时间和结束时间不能为空");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        if (durationHours == null || durationHours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("申请时长必须大于0");
        }

        BigDecimal rangeHours = BigDecimal.valueOf(java.time.Duration.between(startTime, endTime).toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        if (durationHours.compareTo(rangeHours) > 0) {
            throw new BusinessException("申请时长不能超过开始和结束时间范围");
        }
    }
}

package com.officeflow.flow.service.impl;

import com.officeflow.api.attendance.client.AttendanceClient;
import com.officeflow.api.attendance.dto.AttendanceCorrectionDTO;
import com.officeflow.api.attendance.dto.AttendanceLeaveDTO;
import com.officeflow.api.attendance.dto.AttendanceOvertimeDTO;
import com.officeflow.api.user.client.UserAdminClient;
import com.officeflow.api.user.vo.DeptVO;
import com.officeflow.api.user.vo.UserOptionVO;
import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.api.PageResult;
import com.officeflow.common.exception.BusinessException;
import com.officeflow.flow.dto.*;
import com.officeflow.flow.entity.FlowApply;
import com.officeflow.flow.entity.FlowApproveRecord;
import com.officeflow.flow.entity.FlowAttachment;
import com.officeflow.flow.entity.FlowCc;
import com.officeflow.flow.mapper.FlowApplyMapper;
import com.officeflow.flow.mapper.FlowApproveRecordMapper;
import com.officeflow.flow.mapper.FlowAttachmentMapper;
import com.officeflow.flow.mapper.FlowCcMapper;
import com.officeflow.flow.service.FlowService;
import com.officeflow.flow.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowServiceImpl implements FlowService {

    private final FlowApplyMapper flowApplyMapper;
    private final FlowApproveRecordMapper flowApproveRecordMapper;
    private final FlowAttachmentMapper flowAttachmentMapper;
    private final FlowCcMapper flowCcMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserAdminClient userAdminClient;
    private final AttendanceClient attendanceClient;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String AUTO_REJECT_LOCK_KEY = "flow:auto-reject:lock";
    private static final long SYSTEM_APPROVER_ID = 0L;

    @Value("${flow.auto-reject.timeout-hours:48}")
    private int autoRejectTimeoutHours;

    @Override
    @Transactional
    public FlowApplyDetailVO createApply(FlowApplyCreateDTO dto, Long applicantId, Long deptId) {
        String applyType = normalizeApplyType(dto.getApplyType());
        validateApplyRequest(applyType, dto.getStartTime(), dto.getEndTime(), dto.getDurationHours());

        FlowApply apply = new FlowApply();
        apply.setApplyNo(generateApplyNo());
        apply.setApplicantId(applicantId);
        apply.setApplicantDeptId(deptId);

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        if ("LEAVE".equals(dto.getApplyType()) || "OVERTIME".equals(dto.getApplyType())) {
            if (dto.getStartTime().isBefore(now)) {
                throw new BusinessException("请假和加班申请的开始时间不能早于当前时间");
            }
            if (dto.getEndTime().isBefore(now)) {
                throw new BusinessException("请假和加班申请的结束时间不能早于当前时间");
            }
        } else if ("CORRECTION".equals(dto.getApplyType())) {
            if (dto.getStartTime().toLocalDate().isAfter(today)) {
                throw new BusinessException("补卡申请只能针对今天及以前的日期");
            }
        }

        Long managerId = getManagerIdByUserId(applicantId);
        if (managerId == null) {
            throw new BusinessException("未找到直属领导，无法提交申请");
        }
        apply.setApproverId(managerId);
        apply.setApplyType(applyType);
        apply.setTitle(dto.getTitle());
        apply.setReason(dto.getReason());
        apply.setStartTime(dto.getStartTime());
        apply.setEndTime(dto.getEndTime());
        
        if ("LEAVE".equals(applyType)) {
            LocalDateTime start = dto.getStartTime();
            LocalDateTime end = dto.getEndTime();
            LocalDateTime adjustedEnd = end;
            if (end.getHour() == 0 && end.getMinute() == 0 && end.getSecond() == 0 && end.isAfter(start)) {
                adjustedEnd = end.minusSeconds(1);
            }
            long days = java.time.temporal.ChronoUnit.DAYS.between(start.toLocalDate(), adjustedEnd.toLocalDate()) + 1;
            apply.setDurationHours(BigDecimal.valueOf(days * 8));
        } else if ("OVERTIME".equals(applyType)) {
            long minutes = java.time.Duration.between(dto.getStartTime(), dto.getEndTime()).toMinutes();
            apply.setDurationHours(BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 1, java.math.RoundingMode.HALF_UP));
        } else {
            apply.setDurationHours(dto.getDurationHours() != null ? dto.getDurationHours() : BigDecimal.ZERO);
        }
        
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

        if (!CollectionUtils.isEmpty(dto.getAttachmentIds())) {
            flowAttachmentMapper.updateFlowApplyId(dto.getAttachmentIds(), apply.getId());
        }

        return flowApplyMapper.selectDetailById(apply.getId());
    }

    @Override
    public PageResult<FlowApplyListVO> getMyApplies(FlowApplyQueryDTO dto, Long userId) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        dto.setOffset(offset);

        List<FlowApplyListVO> records = flowApplyMapper.selectUserApplies(dto, userId);
        resolveApproverNames(records);
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
        detail.setAttachments(buildAttachmentVOs(flowAttachmentMapper.selectByApplyId(id)));
        resolveDetailNames(detail);

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

        if ("CORRECTION".equalsIgnoreCase(apply.getApplyType())) {
            flowApplyMapper.updateCorrectionStatusByFlowApplyId(apply.getId(), "REVOKED");
        }
    }

    @Override
    public PageResult<FlowPendingVO> getPendingApplies(FlowApplyQueryDTO dto, Long approverId, Long deptId) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        dto.setOffset(offset);

        List<FlowPendingVO> records = flowApplyMapper.selectPendingApplies(dto, approverId, deptId);
        resolvePendingNames(records);
        Long total = flowApplyMapper.countPendingApplies(dto, approverId, deptId);

        return PageResult.of(total, dto.getPageNum(), dto.getPageSize(), records);
    }

    @Override
    public PageResult<FlowProcessedVO> getProcessedApplies(FlowApplyQueryDTO dto, Long approverId, Long deptId) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        dto.setOffset(offset);

        List<FlowProcessedVO> records = flowApplyMapper.selectProcessedApplies(dto, approverId, deptId);
        resolveProcessedNames(records);
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
        String applyType = normalizeApplyType(apply.getApplyType());
        if ("LEAVE".equals(applyType) || "OVERTIME".equals(applyType)) {
            LocalDateTime now = LocalDateTime.now();
            if (dto.getStartTime().isBefore(now)) {
                throw new BusinessException("请假和加班申请的开始时间不能早于当前时间");
            }
            if (dto.getEndTime().isBefore(now)) {
                throw new BusinessException("请假和加班申请的结束时间不能早于当前时间");
            }
        }
        validateApplyRequest(applyType, dto.getStartTime(), dto.getEndTime(), dto.getDurationHours());

        apply.setTitle(dto.getTitle());
        apply.setReason(dto.getReason());
        apply.setStartTime(dto.getStartTime());
        apply.setEndTime(dto.getEndTime());
        
        if ("LEAVE".equals(applyType)) {
            LocalDateTime start = dto.getStartTime();
            LocalDateTime end = dto.getEndTime();
            LocalDateTime adjustedEnd = end;
            if (end.getHour() == 0 && end.getMinute() == 0 && end.getSecond() == 0 && end.isAfter(start)) {
                adjustedEnd = end.minusSeconds(1);
            }
            long days = java.time.temporal.ChronoUnit.DAYS.between(start.toLocalDate(), adjustedEnd.toLocalDate()) + 1;
            apply.setDurationHours(BigDecimal.valueOf(days * 8));
        } else if ("OVERTIME".equals(applyType)) {
            long minutes = java.time.Duration.between(dto.getStartTime(), dto.getEndTime()).toMinutes();
            apply.setDurationHours(BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 1, java.math.RoundingMode.HALF_UP));
        } else {
            apply.setDurationHours(dto.getDurationHours());
        }
        
        flowApplyMapper.update(apply);

        if (dto.getAttachmentIds() != null) {
            flowAttachmentMapper.deleteByApplyId(id);
            if (!CollectionUtils.isEmpty(dto.getAttachmentIds())) {
                flowAttachmentMapper.updateFlowApplyId(dto.getAttachmentIds(), id);
            }
        }
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
        flowAttachmentMapper.deleteByApplyId(id);
    }

    @Override
    public PageResult<FlowApprovedVO> getAllApprovedApplies(FlowApplyQueryDTO dto, Long deptId) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        dto.setOffset(offset);

        List<FlowApprovedVO> records = flowApplyMapper.selectAllApproved(dto, deptId);
        resolveApprovedNames(records);
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

        // 根据类型通知考勤服务
        if ("CORRECTION".equalsIgnoreCase(apply.getApplyType())) {
            AttendanceCorrectionDTO dtoCorrection = new AttendanceCorrectionDTO();
            dtoCorrection.setFlowApplyId(id);
            dtoCorrection.setDeptId(apply.getApplicantDeptId());
            attendanceClient.updateAttendanceForCorrection(dtoCorrection);
        } else if ("LEAVE".equalsIgnoreCase(apply.getApplyType())) {
            if (isFullDayLeave(apply) && apply.getStartTime() != null && apply.getEndTime() != null) {
                java.time.LocalDate cur = apply.getStartTime().toLocalDate();
                java.time.LocalDate end = apply.getEndTime().toLocalDate();
                while (!cur.isAfter(end)) {
                    AttendanceLeaveDTO dtoLeave = new AttendanceLeaveDTO();
                    dtoLeave.setUserId(apply.getApplicantId());
                    dtoLeave.setDeptId(apply.getApplicantDeptId());
                    dtoLeave.setWorkDate(cur);
                    attendanceClient.updateAttendanceForLeave(dtoLeave);
                    cur = cur.plusDays(1);
                }
            }
        } else if ("OVERTIME".equalsIgnoreCase(apply.getApplyType())) {
            AttendanceOvertimeDTO dtoOvertime = new AttendanceOvertimeDTO();
            dtoOvertime.setUserId(apply.getApplicantId());
            dtoOvertime.setDeptId(apply.getApplicantDeptId());
            dtoOvertime.setWorkDate(apply.getStartTime() != null ? apply.getStartTime().toLocalDate() : now.toLocalDate());
            dtoOvertime.setDurationHours(apply.getDurationHours());
            attendanceClient.updateAttendanceForOvertime(dtoOvertime);
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

        doReject(apply, dto.getComment(), approverId);
    }

    private void doReject(FlowApply apply, String comment, Long approverId) {
        flowApplyMapper.updateStatus(apply.getId(), "REJECTED", null);

        FlowApproveRecord record = new FlowApproveRecord();
        record.setFlowApplyId(apply.getId());
        record.setApproverId(approverId);
        record.setAction("REJECT");
        record.setComment(comment);
        record.setApprovedAt(LocalDateTime.now());
        flowApproveRecordMapper.insert(record);

        if ("CORRECTION".equalsIgnoreCase(apply.getApplyType())) {
            flowApplyMapper.updateCorrectionStatusByFlowApplyId(apply.getId(), "REJECTED");
        }
    }

    @Override
    @SuppressWarnings("null")
    public int autoRejectOverdueApplies() {
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(AUTO_REJECT_LOCK_KEY, "1", Duration.ofMinutes(5));
        if (!Boolean.TRUE.equals(locked)) {
            log.debug("Auto-reject task skipped — another instance is running.");
            return 0;
        }

        try {
            List<FlowApply> overdue = flowApplyMapper.selectOverduePendingApplies(autoRejectTimeoutHours);
            if (CollectionUtils.isEmpty(overdue)) {
                return 0;
            }

            int count = 0;
            for (FlowApply apply : overdue) {
                try {
                    doReject(apply, "审批超时（超过" + autoRejectTimeoutHours + "小时未处理），系统自动驳回",
                            SYSTEM_APPROVER_ID);
                    count++;
                    log.info("Auto-rejected overdue apply: id={}, applyNo={}, approverId={}",
                            apply.getId(), apply.getApplyNo(), apply.getApproverId());
                } catch (Exception e) {
                    log.error("Failed to auto-reject apply: id={}", apply.getId(), e);
                }
            }
            return count;
        } finally {
            stringRedisTemplate.delete(AUTO_REJECT_LOCK_KEY);
        }
    }

    private String generateApplyNo() {
        String today = LocalDate.now().format(DATE_FORMAT);
        String key = "flow:apply:seq:" + today;
        Long seq = stringRedisTemplate.opsForValue().increment(key);
        return "FL" + today + String.format("%06d", seq != null ? seq : 1);
    }

    private String normalizeApplyType(String applyType) {
        return applyType == null ? "" : applyType.trim().toUpperCase();
    }

    private boolean isFullDayLeave(FlowApply apply) {
        return apply.getDurationHours() != null && apply.getDurationHours().compareTo(BigDecimal.valueOf(8)) >= 0;
    }

    private void validateApplyRequest(String applyType, LocalDateTime startTime, LocalDateTime endTime, BigDecimal durationHours) {
        if (!"LEAVE".equals(applyType) && !"OVERTIME".equals(applyType) && !"CORRECTION".equals(applyType)) {
            throw new BusinessException("不支持的申请类型");
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

    private Long getManagerIdByUserId(Long userId) {
        try {
            Map<Long, UserOptionVO> userMap = buildUserOptionMap();
            UserOptionVO user = userMap.get(userId);
            if (user != null) {
                return user.getManagerId();
            }
        } catch (Exception e) {
            log.warn("Failed to get managerId from user-service for userId={}", userId, e);
        }
        return null;
    }

    private Map<Long, String> buildUserDisplayNameMap() {
        try {
            Map<Long, String> map = new HashMap<>();
            ApiResponse<List<UserOptionVO>> response = userAdminClient.getUserOptions();
            if (response != null && response.data() != null) {
                for (UserOptionVO user : response.data()) {
                    String displayName = user.getRealName() != null ? user.getRealName() : user.getUsername();
                    map.put(user.getId(), displayName);
                }
            }
            return map;
        } catch (Exception e) {
            log.warn("Failed to get user list from user-service", e);
            return Map.of();
        }
    }

    private Map<Long, UserOptionVO> buildUserOptionMap() {
        try {
            Map<Long, UserOptionVO> map = new HashMap<>();
            ApiResponse<List<UserOptionVO>> response = userAdminClient.getUserOptions();
            if (response != null && response.data() != null) {
                for (UserOptionVO user : response.data()) {
                    map.put(user.getId(), user);
                }
            }
            return map;
        } catch (Exception e) {
            log.warn("Failed to get user options from user-service", e);
            return Map.of();
        }
    }

    private Map<Long, String> buildDeptNameMap() {
        try {
            Map<Long, String> map = new HashMap<>();
            ApiResponse<List<DeptVO>> response = userAdminClient.getDeptList();
            if (response != null && response.data() != null) {
                for (DeptVO dept : response.data()) {
                    map.put(dept.getId(), dept.getDeptName());
                }
            }
            return map;
        } catch (Exception e) {
            log.warn("Failed to get dept list from user-service", e);
            return Map.of();
        }
    }

    private void resolveApproverNames(List<FlowApplyListVO> records) {
        if (records.isEmpty()) return;
        Map<Long, String> userNames = buildUserDisplayNameMap();
        for (FlowApplyListVO vo : records) {
            if (vo.getApproverId() != null) {
                vo.setApproverName(userNames.getOrDefault(vo.getApproverId(), ""));
            }
        }
    }

    private void resolveDetailNames(FlowApplyDetailVO detail) {
        if (detail == null) return;
        Map<Long, String> userNames = buildUserDisplayNameMap();
        Map<Long, String> deptNames = buildDeptNameMap();
        detail.setApplicantName(userNames.getOrDefault(detail.getApplicantId(), ""));
        detail.setApplicantDeptName(deptNames.getOrDefault(detail.getApplicantDeptId(), ""));
        detail.setApproverName(userNames.getOrDefault(detail.getApproverId(), ""));
        if (detail.getApproveRecords() != null) {
            for (FlowApproveRecordVO record : detail.getApproveRecords()) {
                record.setApproverName(userNames.getOrDefault(record.getApproverId(), ""));
            }
        }
    }

    private void resolvePendingNames(List<FlowPendingVO> records) {
        if (records.isEmpty()) return;
        Map<Long, String> userNames = buildUserDisplayNameMap();
        Map<Long, String> deptNames = buildDeptNameMap();
        for (FlowPendingVO vo : records) {
            vo.setApplicantName(userNames.getOrDefault(vo.getApplicantId(), ""));
            vo.setApplicantDeptName(deptNames.getOrDefault(vo.getApplicantDeptId(), ""));
        }
    }

    private void resolveProcessedNames(List<FlowProcessedVO> records) {
        if (records.isEmpty()) return;
        Map<Long, String> userNames = buildUserDisplayNameMap();
        for (FlowProcessedVO vo : records) {
            vo.setApplicantName(userNames.getOrDefault(vo.getApplicantId(), ""));
        }
    }

    private List<AttachmentVO> buildAttachmentVOs(List<FlowAttachment> attachments) {
        if (CollectionUtils.isEmpty(attachments)) {
            return List.of();
        }
        List<AttachmentVO> vos = new ArrayList<>();
        for (FlowAttachment att : attachments) {
            AttachmentVO vo = new AttachmentVO();
            vo.setId(att.getId());
            vo.setFlowApplyId(att.getFlowApplyId());
            vo.setFileName(att.getFileName());
            vo.setFileUrl(att.getFileUrl());
            vo.setFileSize(att.getFileSize());
            vo.setFileType(att.getFileType());
            vos.add(vo);
        }
        return vos;
    }

    private void resolveApprovedNames(List<FlowApprovedVO> records) {
        if (records.isEmpty()) return;
        Map<Long, String> userNames = buildUserDisplayNameMap();
        Map<Long, String> deptNames = buildDeptNameMap();
        for (FlowApprovedVO vo : records) {
            vo.setApplicantName(userNames.getOrDefault(vo.getApplicantId(), ""));
            vo.setApplicantDeptName(deptNames.getOrDefault(vo.getApplicantDeptId(), ""));
            vo.setApproverName(userNames.getOrDefault(vo.getApproverId(), ""));
        }
    }
}

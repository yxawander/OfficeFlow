package com.officeflow.notice.service.impl;

import com.officeflow.api.user.client.UserAdminClient;
import com.officeflow.api.user.vo.DeptVO;
import com.officeflow.api.user.vo.UserOptionVO;
import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.api.PageResult;
import com.officeflow.common.exception.BusinessException;
import com.officeflow.common.api.ResultCode;
import com.officeflow.notice.dto.*;
import com.officeflow.notice.entity.Notice;
import com.officeflow.notice.entity.NoticeScope;
import com.officeflow.notice.entity.NoticeRead;
import com.officeflow.notice.mapper.NoticeMapper;
import com.officeflow.notice.mapper.NoticeReadMapper;
import com.officeflow.notice.mapper.NoticeScopeMapper;
import com.officeflow.notice.service.NoticeService;
import com.officeflow.notice.vo.AdminNoticeListVO;
import com.officeflow.notice.vo.NoticeDetailVO;
import com.officeflow.notice.vo.NoticeListVO;
import com.officeflow.notice.vo.NoticeReadDetailVO;
import com.officeflow.notice.vo.UnreadCountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeMapper noticeMapper;
    private final NoticeScopeMapper noticeScopeMapper;
    private final NoticeReadMapper noticeReadMapper;
    private final UserAdminClient userAdminClient;
    @Override
    public PageResult<NoticeListVO> getNoticeList(NoticeQueryDTO dto, Long userId, Long deptId, List<String> roles) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        dto.setOffset(offset);

        List<NoticeListVO> records = noticeMapper.selectUserNoticeList(dto, userId, deptId, roles);
        Long total = noticeMapper.countUserNoticeList(dto, userId, deptId, roles);

        return PageResult.of(total, dto.getPageNum(), dto.getPageSize(), records);
    }

    @Override
    @Transactional
    public NoticeDetailVO getNoticeDetail(Long id, Long userId, String ip) {
        NoticeDetailVO detail = noticeMapper.selectDetailById(id, userId);
        if (detail == null) {
            throw new BusinessException("公告不存在");
        }

        noticeMapper.incrementViewCount(id);

        if (detail.getReadStatus() == null || detail.getReadStatus() == 0) {
            NoticeRead noticeRead = new NoticeRead();
            noticeRead.setNoticeId(id);
            noticeRead.setUserId(userId);
            noticeRead.setReadStatus((byte) 1);
            noticeRead.setReadAt(LocalDateTime.now());
            noticeRead.setReadIp(ip);
            noticeReadMapper.insert(noticeRead);
            noticeMapper.incrementReadCount(id);
            detail.setReadStatus(1);
            detail.setReadAt(LocalDateTime.now());
        }

        return detail;
    }

    @Override
    public NoticeDetailVO previewNotice(Long id, Long userId) {
        NoticeDetailVO detail = noticeMapper.selectDetailById(id, userId);
        if (detail == null) {
            throw new BusinessException("公告不存在");
        }
        return detail;
    }

    @Override
    public Boolean setReadStatus(Long id, NoticeReadStatusDTO dto, Long userId, String ip) {
        noticeReadMapper.updateReadStatus(id, userId, dto.getReadStatus().byteValue(), ip);
        return true;
    }

    @Override
    @Transactional
    public Integer batchRead(BatchReadDTO dto, Long userId, String ip) {
        List<NoticeRead> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Long noticeId : dto.getNoticeIds()) {
            NoticeRead noticeRead = new NoticeRead();
            noticeRead.setNoticeId(noticeId);
            noticeRead.setUserId(userId);
            noticeRead.setReadStatus((byte) 1);
            noticeRead.setReadAt(now);
            noticeRead.setReadIp(ip);
            list.add(noticeRead);
        }

        noticeReadMapper.batchInsert(list);

        List<NoticeReadDetailVO> readDetails = new ArrayList<>();
        for (Long noticeId : dto.getNoticeIds()) {
            NoticeRead existing = noticeReadMapper.selectByNoticeIdAndUserId(noticeId, userId);
            if (existing != null && existing.getReadStatus() == 0) {
                noticeMapper.incrementReadCount(noticeId);
            }
        }

        return list.size();
    }

    @Override
    public UnreadCountVO getUnreadCount(Long userId, String noticeType, String priority) {
        UnreadCountVO vo = new UnreadCountVO();

        if (noticeType != null && !noticeType.isEmpty() || priority != null && !priority.isEmpty()) {
            vo.setTotal(noticeReadMapper.countUnreadByUserIdAndFilters(userId, noticeType, priority));
            vo.setByType(new HashMap<>());
            vo.setByPriority(new HashMap<>());
        } else {
            vo.setTotal(noticeReadMapper.countUnreadByUserId(userId));
            vo.setByType(noticeReadMapper.countUnreadByType(userId));
            vo.setByPriority(noticeReadMapper.countUnreadByPriority(userId));
        }

        return vo;
    }

    @Override
    @Transactional
    public Long createNotice(NoticeCreateDTO dto, Long userId, String username) {
        Notice notice = new Notice();
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setNoticeType(dto.getNoticeType());
        notice.setPriority(dto.getPriority());
        notice.setPublisherId(userId);
        notice.setPublisherName(username);
        notice.setExpireTime(dto.getExpireTime());
        notice.setStatus("DRAFT");
        notice.setReadCount(0);
        notice.setViewCount(0);
        notice.setIsDeleted((byte) 0);

        noticeMapper.insert(notice);

        List<NoticeScope> scopes = new ArrayList<>();
        for (NoticeScopeDTO scopeDTO : dto.getScopes()) {
            NoticeScope scope = new NoticeScope();
            scope.setNoticeId(notice.getId());
            scope.setScopeType(scopeDTO.getScopeType());
            scope.setScopeId(scopeDTO.getScopeId());
            scopes.add(scope);
        }
        if (!CollectionUtils.isEmpty(scopes)) {
            for (NoticeScope scope : scopes) {
                noticeScopeMapper.insert(scope);
            }
        }

        return notice.getId();
    }

    @Override
    @Transactional
    public Boolean updateNotice(Long id, NoticeUpdateDTO dto) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException("公告不存在");
        }

        if (!"DRAFT".equals(notice.getStatus()) && !"OFFLINE".equals(notice.getStatus())) {
            throw new BusinessException("仅草稿或已下线状态可编辑");
        }

        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setNoticeType(dto.getNoticeType());
        notice.setPriority(dto.getPriority());
        notice.setExpireTime(dto.getExpireTime());

        noticeMapper.updateById(notice);

        noticeScopeMapper.deleteByNoticeId(id);
        for (NoticeScopeDTO scopeDTO : dto.getScopes()) {
            NoticeScope scope = new NoticeScope();
            scope.setNoticeId(id);
            scope.setScopeType(scopeDTO.getScopeType());
            scope.setScopeId(scopeDTO.getScopeId());
            noticeScopeMapper.insert(scope);
        }

        return true;
    }

    @Override
    public Boolean publishNotice(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException("公告不存在");
        }

        if (!"DRAFT".equals(notice.getStatus()) && !"OFFLINE".equals(notice.getStatus())) {
            throw new BusinessException("仅草稿或已下线状态可发布");
        }

        noticeMapper.updateStatusById(id, "PUBLISHED");
        return true;
    }

    @Override
    public Boolean offlineNotice(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException("公告不存在");
        }

        if (!"PUBLISHED".equals(notice.getStatus())) {
            throw new BusinessException("仅已发布状态可下线");
        }

        noticeMapper.updateStatusById(id, "OFFLINE");
        return true;
    }

    @Override
    public Boolean deleteNotice(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException("公告不存在");
        }

        if (!"DRAFT".equals(notice.getStatus()) && !"OFFLINE".equals(notice.getStatus())) {
            throw new BusinessException("仅草稿或已下线状态可删除");
        }

        noticeMapper.deleteById(id);
        noticeScopeMapper.deleteByNoticeId(id);
        return true;
    }

    @Override
    public PageResult<AdminNoticeListVO> getAdminNoticeList(NoticeQueryDTO dto) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        dto.setOffset(offset);

        Long totalActiveUsers = getTotalActiveUsers();
        List<AdminNoticeListVO> records = noticeMapper.selectAdminNoticeList(dto, totalActiveUsers);
        Long total = noticeMapper.countAdminNoticeList(dto);

        return PageResult.of(total, dto.getPageNum(), dto.getPageSize(), records);
    }

    @Override
    public NoticeReadDetailVO getNoticeReadDetail(Long id) {
        Long totalActiveUsers = getTotalActiveUsers();
        NoticeReadDetailVO detail = noticeReadMapper.selectReadDetailById(id, totalActiveUsers);
        if (detail == null) {
            throw new BusinessException("公告不存在");
        }

        detail.setDeptStats(computeDeptStats(id));
        return detail;
    }

    private Long getTotalActiveUsers() {
        try {
            ApiResponse<PageResult<com.officeflow.api.user.vo.UserVO>> response =
                    userAdminClient.getUserPage(null, null, 1, 1, 1);
            if (response != null && response.data() != null) {
                return response.data().total();
            }
        } catch (Exception e) {
            log.warn("Failed to get total active users from user-service, using fallback", e);
        }
        return 1L;
    }

    private List<NoticeReadDetailVO.DeptStatVO> computeDeptStats(Long noticeId) {
        try {
            ApiResponse<List<DeptVO>> deptResponse = userAdminClient.getDeptList();
            List<DeptVO> depts = (deptResponse != null && deptResponse.data() != null)
                    ? deptResponse.data() : List.of();

            ApiResponse<List<UserOptionVO>> userResponse = userAdminClient.getUserOptions();
            List<UserOptionVO> users = (userResponse != null && userResponse.data() != null)
                    ? userResponse.data() : List.of();

            Map<Long, Long> deptTotalUsers = new HashMap<>();
            for (UserOptionVO user : users) {
                if (user.getDeptId() != null) {
                    deptTotalUsers.merge(user.getDeptId(), 1L, Long::sum);
                }
            }

            List<Long> readUserIds = noticeReadMapper.selectReadUserIdsByNoticeId(noticeId);
            Set<Long> readUserSet = new HashSet<>(readUserIds);

            Map<Long, Long> deptReadUsers = new HashMap<>();
            for (UserOptionVO user : users) {
                if (user.getDeptId() != null && readUserSet.contains(user.getId())) {
                    deptReadUsers.merge(user.getDeptId(), 1L, Long::sum);
                }
            }

            List<NoticeReadDetailVO.DeptStatVO> stats = new ArrayList<>();
            for (DeptVO dept : depts) {
                NoticeReadDetailVO.DeptStatVO stat = new NoticeReadDetailVO.DeptStatVO();
                stat.setDeptId(dept.getId());
                stat.setDeptName(dept.getDeptName());
                long total = deptTotalUsers.getOrDefault(dept.getId(), 0L);
                long read = deptReadUsers.getOrDefault(dept.getId(), 0L);
                stat.setTotalUsers(total);
                stat.setReadUsers(read);
                stat.setUnreadUsers(total - read);
                if (total == 0) {
                    stat.setReadRate(BigDecimal.ZERO);
                } else {
                    stat.setReadRate(BigDecimal.valueOf(read * 100.0 / total)
                            .setScale(2, RoundingMode.HALF_UP));
                }
                stats.add(stat);
            }
            return stats;
        } catch (Exception e) {
            log.warn("Failed to compute department stats via user-service, returning empty list", e);
            return List.of();
        }
    }
}
package com.officeflow.notice.service;

import com.officeflow.common.api.PageResult;
import com.officeflow.notice.dto.BatchReadDTO;
import com.officeflow.notice.dto.NoticeCreateDTO;
import com.officeflow.notice.dto.NoticeQueryDTO;
import com.officeflow.notice.dto.NoticeReadStatusDTO;
import com.officeflow.notice.dto.NoticeScopeDTO;
import com.officeflow.notice.dto.NoticeUpdateDTO;
import com.officeflow.notice.entity.NoticeScope;
import com.officeflow.notice.entity.NoticeRead;
import com.officeflow.notice.vo.AdminNoticeListVO;
import com.officeflow.notice.vo.NoticeDetailVO;
import com.officeflow.notice.vo.NoticeListVO;
import com.officeflow.notice.vo.NoticeReadDetailVO;
import com.officeflow.notice.vo.UnreadCountVO;

import java.util.List;

public interface NoticeService {

    PageResult<NoticeListVO> getNoticeList(NoticeQueryDTO dto, Long userId, Long deptId, List<String> roles);

    NoticeDetailVO getNoticeDetail(Long id, Long userId, String ip);

    NoticeDetailVO previewNotice(Long id, Long userId);

    Boolean setReadStatus(Long id, NoticeReadStatusDTO dto, Long userId, String ip);

    Integer batchRead(BatchReadDTO dto, Long userId, String ip);

    UnreadCountVO getUnreadCount(Long userId, String noticeType, String priority);

    Long createNotice(NoticeCreateDTO dto, Long userId, String username);

    Boolean updateNotice(Long id, NoticeUpdateDTO dto);

    Boolean publishNotice(Long id);

    Boolean offlineNotice(Long id);

    Boolean deleteNotice(Long id);

    PageResult<AdminNoticeListVO> getAdminNoticeList(NoticeQueryDTO dto);

    NoticeReadDetailVO getNoticeReadDetail(Long id);

    int autoPublishScheduledNotices();
}
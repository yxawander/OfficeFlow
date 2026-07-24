package com.officeflow.notice.service;

import com.officeflow.common.api.PageResult;
import com.officeflow.notice.document.NoticeDocument;
import com.officeflow.notice.dto.NoticeQueryDTO;
import com.officeflow.notice.vo.AdminNoticeListVO;
import com.officeflow.notice.vo.NoticeListVO;

public interface NoticeSearchService {

    /**
     * 用户端关键词搜索（ES优先，失败fallback到MySQL LIKE）
     */
    PageResult<NoticeListVO> searchUser(String keyword, NoticeQueryDTO dto, Long userId, Long deptId);

    /**
     * 管理端关键词搜索（ES优先，失败fallback到MySQL LIKE）
     */
    PageResult<AdminNoticeListVO> searchAdmin(String keyword, NoticeQueryDTO dto);

    /**
     * 异步索引文档
     */
    void indexAsync(NoticeDocument doc);

    /**
     * 异步删除文档
     */
    void deleteAsync(Long id);

    /**
     * 启动时全量同步
     */
    int syncAll();

    /**
     * 定时修复：同步最近N分钟变更
     */
    int repairRecent(int minutes);
}

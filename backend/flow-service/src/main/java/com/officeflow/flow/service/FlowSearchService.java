package com.officeflow.flow.service;

import com.officeflow.common.api.PageResult;
import com.officeflow.flow.document.FlowApplyDocument;
import com.officeflow.flow.dto.FlowApplyQueryDTO;
import com.officeflow.flow.vo.FlowApplyListVO;

import java.util.List;

public interface FlowSearchService {

    /**
     * 关键词搜索审批申请（ES优先，失败fallback到MySQL LIKE）
     */
    PageResult<FlowApplyListVO> search(String keyword, FlowApplyQueryDTO queryDTO, Long userId, Long deptId);

    /**
     * 异步索引文档
     */
    void indexAsync(FlowApplyDocument doc);

    /**
     * 异步删除文档
     */
    void deleteAsync(Long id);

    /**
     * 定时修复：全量同步最近N分钟变更的审批单
     */
    int repairRecent(int minutes);

    /**
     * 启动时全量同步所有未删除的审批单到ES
     */
    int syncAll();
}

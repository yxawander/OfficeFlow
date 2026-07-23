package com.officeflow.api.notice.client;

import com.officeflow.api.notice.dto.BatchReadDTO;
import com.officeflow.api.notice.dto.NoticeQueryDTO;
import com.officeflow.api.notice.dto.NoticeReadStatusDTO;
import com.officeflow.api.notice.vo.NoticeDetailVO;
import com.officeflow.api.notice.vo.NoticeListVO;
import com.officeflow.api.notice.vo.UnreadCountVO;
import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.api.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "notice-service", contextId = "noticeClient", path = "/api/notice")
public interface NoticeClient {

    @GetMapping("/notices")
    ApiResponse<PageResult<NoticeListVO>> getNoticeList(@SpringQueryMap NoticeQueryDTO dto,
                                                         @RequestHeader("X-Login-User-Id") Long userId,
                                                         @RequestHeader(value = "X-Login-Dept-Id", required = false) Long deptId);

    @GetMapping("/notices/{id}")
    ApiResponse<NoticeDetailVO> getNoticeDetail(@PathVariable Long id,
                                                 @RequestHeader("X-Login-User-Id") Long userId);

    @GetMapping("/notices/{id}/preview")
    ApiResponse<NoticeDetailVO> previewNotice(@PathVariable Long id,
                                               @RequestHeader("X-Login-User-Id") Long userId);

    @PostMapping("/notices/{id}/read-status")
    ApiResponse<Boolean> setReadStatus(@PathVariable Long id,
                                        @RequestBody NoticeReadStatusDTO dto,
                                        @RequestHeader("X-Login-User-Id") Long userId);

    @PostMapping("/notices/batch-read")
    ApiResponse<Integer> batchRead(@RequestBody BatchReadDTO dto,
                                    @RequestHeader("X-Login-User-Id") Long userId);

    @GetMapping("/notices/unread-count")
    ApiResponse<UnreadCountVO> getUnreadCount(@RequestParam(required = false) String noticeType,
                                               @RequestParam(required = false) String priority,
                                               @RequestHeader("X-Login-User-Id") Long userId);
}

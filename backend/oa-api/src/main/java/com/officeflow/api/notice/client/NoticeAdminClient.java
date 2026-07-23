package com.officeflow.api.notice.client;

import com.officeflow.api.notice.dto.NoticeCreateDTO;
import com.officeflow.api.notice.dto.NoticeQueryDTO;
import com.officeflow.api.notice.dto.NoticeUpdateDTO;
import com.officeflow.api.notice.vo.AdminNoticeListVO;
import com.officeflow.api.notice.vo.NoticeReadDetailVO;
import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.api.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "notice-service", contextId = "noticeAdminClient", path = "/api/notice/admin")
public interface NoticeAdminClient {

    @PostMapping("/notices")
    ApiResponse<Long> createNotice(@RequestBody NoticeCreateDTO dto,
                                    @RequestHeader("X-Login-User-Id") Long userId,
                                    @RequestHeader("X-Login-Username") String username);

    @PutMapping("/notices/{id}")
    ApiResponse<Boolean> updateNotice(@PathVariable Long id,
                                       @RequestBody NoticeUpdateDTO dto);

    @PostMapping("/notices/{id}/publish")
    ApiResponse<Long> publishNotice(@PathVariable Long id);

    @PostMapping("/notices/{id}/offline")
    ApiResponse<Long> offlineNotice(@PathVariable Long id);

    @DeleteMapping("/notices/{id}")
    ApiResponse<Boolean> deleteNotice(@PathVariable Long id);

    @GetMapping("/notices")
    ApiResponse<PageResult<AdminNoticeListVO>> getAdminNoticeList(@SpringQueryMap NoticeQueryDTO dto);

    @GetMapping("/notices/{id}/read-details")
    ApiResponse<NoticeReadDetailVO> getNoticeReadDetail(@PathVariable Long id);
}

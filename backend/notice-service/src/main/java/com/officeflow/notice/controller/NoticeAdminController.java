package com.officeflow.notice.controller;

import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.api.PageResult;
import com.officeflow.common.api.ResultCode;
import com.officeflow.common.constant.CommonConstants;
import com.officeflow.notice.dto.NoticeCreateDTO;
import com.officeflow.notice.dto.NoticeQueryDTO;
import com.officeflow.notice.dto.NoticeUpdateDTO;
import com.officeflow.notice.service.NoticeService;
import com.officeflow.notice.vo.AdminNoticeListVO;
import com.officeflow.notice.vo.NoticeReadDetailVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notice/admin")
@RequiredArgsConstructor
public class NoticeAdminController {

    private final NoticeService noticeService;

    @PostMapping("/notices")
    public ApiResponse<Long> createNotice(@Valid @RequestBody NoticeCreateDTO dto, HttpServletRequest request) {
        Long userId = getUserId(request);
        String username = getUsername(request);
        return ApiResponse.ok(noticeService.createNotice(dto, userId, username));
    }

    @PutMapping("/notices/{id}")
    public ApiResponse<Boolean> updateNotice(@PathVariable Long id,
                                               @Valid @RequestBody NoticeUpdateDTO dto,
                                               HttpServletRequest request) {
        return ApiResponse.ok(noticeService.updateNotice(id, dto));
    }

    @PostMapping("/notices/{id}/publish")
    public ApiResponse<Long> publishNotice(@PathVariable Long id) {
        noticeService.publishNotice(id);
        return ApiResponse.ok(id);
    }

    @PostMapping("/notices/{id}/offline")
    public ApiResponse<Long> offlineNotice(@PathVariable Long id) {
        noticeService.offlineNotice(id);
        return ApiResponse.ok(id);
    }

    @DeleteMapping("/notices/{id}")
    public ApiResponse<Boolean> deleteNotice(@PathVariable Long id) {
        return ApiResponse.ok(noticeService.deleteNotice(id));
    }

    @GetMapping("/notices")
    public ApiResponse<PageResult<AdminNoticeListVO>> getAdminNoticeList(NoticeQueryDTO dto) {
        return ApiResponse.ok(noticeService.getAdminNoticeList(dto));
    }

    @GetMapping("/notices/{id}/read-details")
    public ApiResponse<NoticeReadDetailVO> getNoticeReadDetail(@PathVariable Long id) {
        return ApiResponse.ok(noticeService.getNoticeReadDetail(id));
    }

    private Long getUserId(HttpServletRequest request) {
        String userIdStr = request.getHeader(CommonConstants.LOGIN_USER_ID_HEADER);
        if (userIdStr == null) {
            throw new com.officeflow.common.exception.BusinessException("用户未登录");
        }
        return Long.parseLong(userIdStr);
    }

    private String getUsername(HttpServletRequest request) {
        String username = request.getHeader(CommonConstants.LOGIN_USERNAME_HEADER);
        if (username == null) {
            throw new com.officeflow.common.exception.BusinessException("用户未登录");
        }
        return username;
    }
}
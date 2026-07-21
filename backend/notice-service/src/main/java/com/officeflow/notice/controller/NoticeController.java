package com.officeflow.notice.controller;

import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.api.PageResult;
import com.officeflow.common.api.ResultCode;
import com.officeflow.common.constant.CommonConstants;
import com.officeflow.notice.dto.BatchReadDTO;
import com.officeflow.notice.dto.NoticeQueryDTO;
import com.officeflow.notice.dto.NoticeReadStatusDTO;
import com.officeflow.notice.service.NoticeService;
import com.officeflow.notice.vo.NoticeDetailVO;
import com.officeflow.notice.vo.NoticeListVO;
import com.officeflow.notice.vo.UnreadCountVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/notices")
    public ApiResponse<PageResult<NoticeListVO>> getNoticeList(NoticeQueryDTO dto, HttpServletRequest request) {
        Long userId = getUserId(request);
        Long deptId = getDeptId(request);
        return ApiResponse.ok(noticeService.getNoticeList(dto, userId, deptId, null));
    }

    @GetMapping("/notices/{id}")
    public ApiResponse<NoticeDetailVO> getNoticeDetail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        String ip = getClientIp(request);
        return ApiResponse.ok(noticeService.getNoticeDetail(id, userId, ip));
    }

    @GetMapping("/notices/{id}/preview")
    public ApiResponse<NoticeDetailVO> previewNotice(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.ok(noticeService.previewNotice(id, userId));
    }

    @PostMapping("/notices/{id}/read-status")
    public ApiResponse<Boolean> setReadStatus(@PathVariable Long id,
                                                 @Valid @RequestBody NoticeReadStatusDTO dto,
                                                 HttpServletRequest request) {
        Long userId = getUserId(request);
        String ip = getClientIp(request);
        return ApiResponse.ok(noticeService.setReadStatus(id, dto, userId, ip));
    }

    @PostMapping("/notices/batch-read")
    public ApiResponse<Integer> batchRead(@Valid @RequestBody BatchReadDTO dto, HttpServletRequest request) {
        Long userId = getUserId(request);
        String ip = getClientIp(request);
        return ApiResponse.ok(noticeService.batchRead(dto, userId, ip));
    }

    @GetMapping("/notices/unread-count")
    public ApiResponse<UnreadCountVO> getUnreadCount(@RequestParam(required = false) String noticeType,
                                                       @RequestParam(required = false) String priority,
                                                       HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.ok(noticeService.getUnreadCount(userId, noticeType, priority));
    }

    private Long getUserId(HttpServletRequest request) {
        String userIdStr = request.getHeader(CommonConstants.LOGIN_USER_ID_HEADER);
        if (userIdStr == null) {
            throw new com.officeflow.common.exception.BusinessException("用户未登录");
        }
        return Long.parseLong(userIdStr);
    }

    private Long getDeptId(HttpServletRequest request) {
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip.split(",")[0] : "unknown";
    }
}
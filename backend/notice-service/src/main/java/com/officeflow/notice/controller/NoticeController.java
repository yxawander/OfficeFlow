package com.officeflow.notice.controller;

import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.api.PageResult;
import com.officeflow.common.constant.CommonConstants;
import com.officeflow.common.exception.BusinessException;
import com.officeflow.notice.dto.BatchReadDTO;
import com.officeflow.notice.dto.NoticeQueryDTO;
import com.officeflow.notice.dto.NoticeReadStatusDTO;
import com.officeflow.notice.entity.NoticeAttachment;
import com.officeflow.notice.mapper.NoticeAttachmentMapper;
import com.officeflow.notice.service.NoticeSearchService;
import com.officeflow.notice.service.NoticeService;
import com.officeflow.notice.service.OssService;
import com.officeflow.notice.vo.NoticeDetailVO;
import com.officeflow.notice.vo.NoticeListVO;
import com.officeflow.notice.vo.UnreadCountVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final NoticeSearchService noticeSearchService;
    private final OssService ossService;
    private final NoticeAttachmentMapper noticeAttachmentMapper;

    @GetMapping("/notices/search")
    public ApiResponse<PageResult<NoticeListVO>> searchNotices(@RequestParam(required = false) String keyword,
                                                                NoticeQueryDTO dto,
                                                                HttpServletRequest request) {
        Long userId = getUserId(request);
        Long deptId = getDeptId(request);
        return ApiResponse.ok(noticeSearchService.searchUser(keyword, dto, userId, deptId));
    }

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

    @GetMapping("/attachments/{id}/download")
    public void downloadAttachment(@PathVariable Long id, HttpServletResponse response) {
        NoticeAttachment attachment = noticeAttachmentMapper.selectById(id);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }
        String fileName = attachment.getFileName() != null ? attachment.getFileName() : "file";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(attachment.getFileType() != null ? attachment.getFileType() : "application/octet-stream");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded);
        response.setContentLengthLong(attachment.getFileSize());
        try {
            ossService.downloadToStream(attachment.getOssKey(), response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Long getUserId(HttpServletRequest request) {
        String userIdStr = request.getHeader(CommonConstants.LOGIN_USER_ID_HEADER);
        if (userIdStr == null) {
            throw new com.officeflow.common.exception.BusinessException("用户未登录");
        }
        return Long.parseLong(userIdStr);
    }

    private Long getDeptId(HttpServletRequest request) {
        String deptIdStr = request.getHeader(CommonConstants.LOGIN_DEPT_ID_HEADER);
        if (deptIdStr == null || deptIdStr.isEmpty() || "null".equals(deptIdStr)) {
            return null;
        }
        return Long.parseLong(deptIdStr);
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
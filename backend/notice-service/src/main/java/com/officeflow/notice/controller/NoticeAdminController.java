package com.officeflow.notice.controller;

import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.api.PageResult;
import com.officeflow.common.constant.CommonConstants;
import com.officeflow.notice.dto.NoticeCreateDTO;
import com.officeflow.notice.dto.NoticeQueryDTO;
import com.officeflow.notice.dto.NoticeUpdateDTO;
import com.officeflow.notice.entity.NoticeAttachment;
import com.officeflow.notice.mapper.NoticeAttachmentMapper;
import com.officeflow.notice.service.NoticeService;
import com.officeflow.notice.service.OssService;
import com.officeflow.notice.service.impl.OssServiceImpl;
import com.officeflow.notice.vo.AdminNoticeListVO;
import com.officeflow.notice.vo.AttachmentVO;
import com.officeflow.notice.vo.NoticeReadDetailVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/notice/admin")
@RequiredArgsConstructor
public class NoticeAdminController {

    private final NoticeService noticeService;
    private final OssService ossService;
    private final NoticeAttachmentMapper noticeAttachmentMapper;

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

    @PostMapping("/attachments/upload")
    public ApiResponse<AttachmentVO> uploadAttachment(@RequestParam("file") MultipartFile file,
                                                       HttpServletRequest request) {
        Long userId = getUserId(request);
        String originalName = file.getOriginalFilename();
        String objectKey = OssServiceImpl.generateObjectKey(originalName != null ? originalName : "file");
        String fileUrl = ossService.upload(file, objectKey);

        NoticeAttachment attachment = new NoticeAttachment();
        attachment.setFileName(originalName);
        attachment.setFileUrl(fileUrl);
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        attachment.setOssKey(objectKey);
        attachment.setUploadedBy(userId);
        noticeAttachmentMapper.insert(attachment);

        AttachmentVO vo = new AttachmentVO();
        vo.setId(attachment.getId());
        vo.setFileName(attachment.getFileName());
        vo.setFileUrl(attachment.getFileUrl());
        vo.setFileSize(attachment.getFileSize());
        vo.setFileType(attachment.getFileType());
        return ApiResponse.ok(vo);
    }

    @GetMapping("/attachments/{id}")
    public ApiResponse<String> getAttachmentUrl(@PathVariable Long id) {
        NoticeAttachment attachment = noticeAttachmentMapper.selectById(id);
        if (attachment == null) {
            return ApiResponse.fail("附件不存在");
        }
        return ApiResponse.ok(attachment.getFileUrl());
    }

    @DeleteMapping("/attachments/{id}")
    public ApiResponse<Boolean> deleteAttachment(@PathVariable Long id) {
        NoticeAttachment attachment = noticeAttachmentMapper.selectById(id);
        if (attachment == null) {
            return ApiResponse.fail("附件不存在");
        }
        if (attachment.getOssKey() != null) {
            ossService.delete(attachment.getOssKey());
        }
        noticeAttachmentMapper.deleteById(id);
        return ApiResponse.ok(true);
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
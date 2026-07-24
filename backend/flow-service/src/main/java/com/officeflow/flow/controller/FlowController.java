package com.officeflow.flow.controller;

import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.api.PageResult;
import com.officeflow.common.constant.CommonConstants;
import com.officeflow.common.exception.BusinessException;
import com.officeflow.flow.dto.FlowApplyCreateDTO;
import com.officeflow.flow.dto.FlowApplyQueryDTO;
import com.officeflow.flow.dto.FlowApplyUpdateDTO;
import com.officeflow.flow.entity.FlowAttachment;
import com.officeflow.flow.mapper.FlowAttachmentMapper;
import com.officeflow.flow.service.FlowSearchService;
import com.officeflow.flow.service.FlowService;
import com.officeflow.flow.service.OssService;
import com.officeflow.flow.service.impl.OssServiceImpl;
import com.officeflow.flow.vo.AttachmentVO;
import com.officeflow.flow.vo.FlowApplyDetailVO;
import com.officeflow.flow.vo.FlowApplyListVO;
import com.officeflow.flow.vo.FlowPendingVO;
import com.officeflow.flow.vo.FlowProcessedVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/flow")
@RequiredArgsConstructor
public class FlowController {

    private final FlowService flowService;
    private final OssService ossService;
    private final FlowAttachmentMapper flowAttachmentMapper;
    private final FlowSearchService flowSearchService;

    @PostMapping("/applies")
    public ApiResponse<FlowApplyDetailVO> createApply(@Valid @RequestBody FlowApplyCreateDTO dto,
                                                       HttpServletRequest request) {
        Long userId = getUserId(request);
        Long deptId = getDeptId(request);
        return ApiResponse.ok(flowService.createApply(dto, userId, deptId));
    }

    @GetMapping("/applies/my")
    public ApiResponse<PageResult<FlowApplyListVO>> getMyApplies(FlowApplyQueryDTO dto,
                                                                  HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.ok(flowService.getMyApplies(dto, userId));
    }

    @GetMapping("/applies/search")
    public ApiResponse<PageResult<FlowApplyListVO>> searchApplies(
            @RequestParam(required = false) String keyword,
            FlowApplyQueryDTO dto,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        Long deptId = getDeptId(request);
        return ApiResponse.ok(flowSearchService.search(keyword, dto, userId, deptId));
    }

    @GetMapping("/applies/{id}")
    public ApiResponse<FlowApplyDetailVO> getApplyDetail(@PathVariable Long id) {
        return ApiResponse.ok(flowService.getApplyDetail(id));
    }

    @PutMapping("/applies/{id}")
    public ApiResponse<Void> updateApply(@PathVariable Long id,
                                          @Valid @RequestBody FlowApplyUpdateDTO dto,
                                          HttpServletRequest request) {
        Long userId = getUserId(request);
        flowService.updateApply(id, dto, userId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/applies/{id}")
    public ApiResponse<Void> deleteApply(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        flowService.deleteApply(id, userId);
        return ApiResponse.ok();
    }

    @PutMapping("/applies/{id}/cancel")
    public ApiResponse<Void> cancelApply(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        flowService.cancelApply(id, userId);
        return ApiResponse.ok();
    }

    @GetMapping("/applies/pending")
    public ApiResponse<PageResult<FlowPendingVO>> getPendingApplies(FlowApplyQueryDTO dto,
                                                                    HttpServletRequest request) {
        Long approverId = getUserId(request);
        String rolesStr = request.getHeader(CommonConstants.LOGIN_ROLES_HEADER);
        boolean admin = isAdmin(request);
        Long deptId = admin ? null : getDeptId(request);
        log.info("getPendingApplies: approverId={}, roles={}, isAdmin={}, deptId={}", approverId, rolesStr, admin, deptId);
        PageResult<FlowPendingVO> result = flowService.getPendingApplies(dto, approverId, deptId);
        log.info("getPendingApplies result: total={}, records={}", result.total(), result.records() != null ? result.records().size() : 0);
        return ApiResponse.ok(result);
    }

    @GetMapping("/applies/processed")
    public ApiResponse<PageResult<FlowProcessedVO>> getProcessedApplies(FlowApplyQueryDTO dto,
                                                                         HttpServletRequest request) {
        Long approverId = getUserId(request);
        Long deptId = isAdmin(request) ? null : getDeptId(request);
        return ApiResponse.ok(flowService.getProcessedApplies(dto, approverId, deptId));
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
        if (deptIdStr != null) {
            return Long.parseLong(deptIdStr);
        }
        return null;
    }

    private boolean isAdmin(HttpServletRequest request) {
        String rolesStr = request.getHeader(CommonConstants.LOGIN_ROLES_HEADER);
        if (rolesStr == null) {
            return false;
        }
        String cleaned = rolesStr.replace("[", "").replace("]", "").trim();
        return java.util.Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .anyMatch(r -> "ADMIN".equals(r));
    }

    @PostMapping("/attachments/upload")
    public ApiResponse<AttachmentVO> uploadAttachment(@RequestParam("file") MultipartFile file,
                                                       HttpServletRequest request) {
        Long userId = getUserId(request);
        String objectKey = OssServiceImpl.generateObjectKey(file.getOriginalFilename());
        String fileUrl = ossService.upload(file, objectKey);

        FlowAttachment attachment = new FlowAttachment();
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileUrl(fileUrl);
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        attachment.setOssKey(objectKey);
        attachment.setUploadedBy(userId);
        flowAttachmentMapper.insert(attachment);

        AttachmentVO vo = new AttachmentVO();
        vo.setId(attachment.getId());
        vo.setFileName(attachment.getFileName());
        vo.setFileUrl(attachment.getFileUrl());
        vo.setFileSize(attachment.getFileSize());
        vo.setFileType(attachment.getFileType());
        return ApiResponse.ok(vo);
    }

    @DeleteMapping("/attachments/{id}")
    public ApiResponse<Void> deleteAttachment(@PathVariable Long id,
                                               HttpServletRequest request) {
        Long userId = getUserId(request);
        FlowAttachment attachment = flowAttachmentMapper.selectById(id);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }
        if (!attachment.getUploadedBy().equals(userId)) {
            throw new BusinessException("只能删除自己上传的附件");
        }
        if (attachment.getOssKey() != null) {
            ossService.delete(attachment.getOssKey());
        }
        flowAttachmentMapper.deleteById(id);
        return ApiResponse.ok();
    }

    @GetMapping("/attachments/{id}/download")
    public void downloadAttachment(@PathVariable Long id, HttpServletResponse response) {
        FlowAttachment attachment = flowAttachmentMapper.selectById(id);
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
}

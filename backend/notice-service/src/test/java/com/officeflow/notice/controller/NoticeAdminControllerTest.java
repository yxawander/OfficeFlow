package com.officeflow.notice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.officeflow.common.api.PageResult;
import com.officeflow.common.exception.GlobalExceptionHandler;
import com.officeflow.notice.dto.NoticeCreateDTO;
import com.officeflow.notice.dto.NoticeScopeDTO;
import com.officeflow.notice.dto.NoticeUpdateDTO;
import com.officeflow.notice.mapper.NoticeAttachmentMapper;
import com.officeflow.notice.service.NoticeService;
import com.officeflow.notice.service.OssService;
import com.officeflow.notice.vo.AdminNoticeListVO;
import com.officeflow.notice.vo.NoticeReadDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
@DisplayName("公告管理控制器单元测试")
class NoticeAdminControllerTest {

    @Mock
    private NoticeService noticeService;

    @Mock
    private OssService ossService;

    @Mock
    private NoticeAttachmentMapper noticeAttachmentMapper;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private NoticeCreateDTO createDTO;
    private NoticeUpdateDTO updateDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NoticeAdminController(noticeService, ossService, noticeAttachmentMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        NoticeScopeDTO scopeDTO = new NoticeScopeDTO();
        scopeDTO.setScopeType("DEPT");
        scopeDTO.setScopeId(1L);

        createDTO = new NoticeCreateDTO();
        createDTO.setTitle("测试公告");
        createDTO.setContent("测试内容");
        createDTO.setNoticeType("COMPANY");
        createDTO.setPriority("HIGH");
        createDTO.setExpireTime(LocalDateTime.now().plusDays(7));
        createDTO.setScopes(List.of(scopeDTO));

        updateDTO = new NoticeUpdateDTO();
        updateDTO.setId(1L);
        updateDTO.setTitle("更新标题");
        updateDTO.setContent("更新内容");
        updateDTO.setNoticeType("DEPT");
        updateDTO.setPriority("NORMAL");
        updateDTO.setExpireTime(LocalDateTime.now().plusDays(14));
        updateDTO.setScopes(List.of(scopeDTO));
    }

    @Test
    @DisplayName("创建公告 - 成功")
    void createNotice_Success() throws Exception {
        when(noticeService.createNotice(any(), any(), any())).thenReturn(1L);

        mockMvc.perform(post("/api/notice/admin/notices")
                        .header("X-Login-User-Id", "100")
                        .header("X-Login-Username", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("创建公告 - 用户未登录")
    void createNotice_NotLoggedIn() throws Exception {
        mockMvc.perform(post("/api/notice/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(com.officeflow.common.api.ResultCode.FAIL.code()));
    }

    @Test
    @DisplayName("创建公告 - 用户名缺失")
    void createNotice_UsernameMissing() throws Exception {
        mockMvc.perform(post("/api/notice/admin/notices")
                        .header("X-Login-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(com.officeflow.common.api.ResultCode.FAIL.code()));
    }

    @Test
    @DisplayName("创建公告 - 验证失败 - 标题为空")
    void createNotice_ValidationError_TitleEmpty() throws Exception {
        createDTO.setTitle("");

        mockMvc.perform(post("/api/notice/admin/notices")
                        .header("X-Login-User-Id", "100")
                        .header("X-Login-Username", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(com.officeflow.common.api.ResultCode.PARAM_ERROR.code()));
    }

    @Test
    @DisplayName("创建公告 - 验证失败 - 内容为空")
    void createNotice_ValidationError_ContentEmpty() throws Exception {
        createDTO.setContent(null);

        mockMvc.perform(post("/api/notice/admin/notices")
                        .header("X-Login-User-Id", "100")
                        .header("X-Login-Username", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(com.officeflow.common.api.ResultCode.PARAM_ERROR.code()));
    }

    @Test
    @DisplayName("创建公告 - 验证失败 - 可见范围为空")
    void createNotice_ValidationError_ScopesEmpty() throws Exception {
        createDTO.setScopes(null);

        mockMvc.perform(post("/api/notice/admin/notices")
                        .header("X-Login-User-Id", "100")
                        .header("X-Login-Username", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(com.officeflow.common.api.ResultCode.PARAM_ERROR.code()));
    }

    @Test
    @DisplayName("更新公告 - 成功")
    void updateNotice_Success() throws Exception {
        when(noticeService.updateNotice(any(), any())).thenReturn(true);

        mockMvc.perform(put("/api/notice/admin/notices/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("发布公告 - 成功")
    void publishNotice_Success() throws Exception {
        when(noticeService.publishNotice(any())).thenReturn(true);

        mockMvc.perform(post("/api/notice/admin/notices/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("下线公告 - 成功")
    void offlineNotice_Success() throws Exception {
        when(noticeService.offlineNotice(any())).thenReturn(true);

        mockMvc.perform(post("/api/notice/admin/notices/1/offline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("删除公告 - 成功")
    void deleteNotice_Success() throws Exception {
        when(noticeService.deleteNotice(any())).thenReturn(true);

        mockMvc.perform(delete("/api/notice/admin/notices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("获取管理端公告列表 - 成功")
    void getAdminNoticeList_Success() throws Exception {
        PageResult<AdminNoticeListVO> pageResult = PageResult.of(1, 1, 10, List.of(new AdminNoticeListVO()));
        when(noticeService.getAdminNoticeList(any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/notice/admin/notices")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("获取管理端公告列表 - 带查询条件")
    void getAdminNoticeList_WithFilters() throws Exception {
        PageResult<AdminNoticeListVO> pageResult = PageResult.of(1, 1, 10, List.of(new AdminNoticeListVO()));
        when(noticeService.getAdminNoticeList(any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/notice/admin/notices")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("keyword", "测试")
                        .param("noticeType", "COMPANY")
                        .param("priority", "HIGH")
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("获取管理端公告列表 - 自定义分页参数")
    void getAdminNoticeList_CustomPagination() throws Exception {
        PageResult<AdminNoticeListVO> pageResult = PageResult.of(50, 2, 25, List.of(new AdminNoticeListVO()));
        when(noticeService.getAdminNoticeList(any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/notice/admin/notices")
                        .param("pageNum", "2")
                        .param("pageSize", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pageNum").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(25));
    }

    @Test
    @DisplayName("获取公告阅读详情 - 成功")
    void getNoticeReadDetail_Success() throws Exception {
        NoticeReadDetailVO detailVO = new NoticeReadDetailVO();
        detailVO.setNoticeId(1L);

        when(noticeService.getNoticeReadDetail(any())).thenReturn(detailVO);

        mockMvc.perform(get("/api/notice/admin/notices/1/read-details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.noticeId").value(1));
    }

    @Test
    @DisplayName("更新公告 - 验证失败")
    void updateNotice_ValidationError() throws Exception {
        updateDTO.setTitle("");

        mockMvc.perform(put("/api/notice/admin/notices/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(com.officeflow.common.api.ResultCode.PARAM_ERROR.code()));
    }
}
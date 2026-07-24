package com.officeflow.notice.controller;

import com.officeflow.common.api.PageResult;
import com.officeflow.common.exception.GlobalExceptionHandler;
import com.officeflow.notice.dto.BatchReadDTO;
import com.officeflow.notice.dto.NoticeReadStatusDTO;
import com.officeflow.notice.mapper.NoticeAttachmentMapper;
import com.officeflow.notice.service.NoticeSearchService;
import com.officeflow.notice.service.NoticeService;
import com.officeflow.notice.service.OssService;
import com.officeflow.notice.vo.NoticeDetailVO;
import com.officeflow.notice.vo.NoticeListVO;
import com.officeflow.notice.vo.UnreadCountVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
@DisplayName("公告控制器单元测试")
class NoticeControllerTest {

    @Mock
    private NoticeService noticeService;

    @Mock
    private NoticeSearchService noticeSearchService;

    @Mock
    private OssService ossService;

    @Mock
    private NoticeAttachmentMapper noticeAttachmentMapper;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new NoticeController(noticeService, noticeSearchService, ossService, noticeAttachmentMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("获取公告列表 - 成功")
    void getNoticeList_Success() throws Exception {
        PageResult<NoticeListVO> pageResult = PageResult.of(1, 1, 10, List.of(new NoticeListVO()));
        when(noticeService.getNoticeList(any(), any(), any(), any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/notice/notices")
                        .header("X-Login-User-Id", "100")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("获取公告列表 - 用户未登录")
    void getNoticeList_NotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/notice/notices")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(com.officeflow.common.api.ResultCode.FAIL.code()));
    }

    @Test
    @DisplayName("获取公告详情 - 成功")
    void getNoticeDetail_Success() throws Exception {
        NoticeDetailVO detailVO = new NoticeDetailVO();
        detailVO.setId(1L);
        detailVO.setTitle("测试公告");

        when(noticeService.getNoticeDetail(any(), any(), any())).thenReturn(detailVO);

        mockMvc.perform(get("/api/notice/notices/1")
                        .header("X-Login-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("预览公告 - 成功")
    void previewNotice_Success() throws Exception {
        NoticeDetailVO detailVO = new NoticeDetailVO();
        detailVO.setId(1L);
        detailVO.setTitle("测试公告");

        when(noticeService.previewNotice(any(), any())).thenReturn(detailVO);

        mockMvc.perform(get("/api/notice/notices/1/preview")
                        .header("X-Login-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("设置阅读状态 - 成功")
    void setReadStatus_Success() throws Exception {
        NoticeReadStatusDTO dto = new NoticeReadStatusDTO();
        dto.setReadStatus(1);

        when(noticeService.setReadStatus(any(), any(), any(), any())).thenReturn(true);

        mockMvc.perform(post("/api/notice/notices/1/read-status")
                        .header("X-Login-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("设置阅读状态 - 验证失败")
    void setReadStatus_ValidationError() throws Exception {
        mockMvc.perform(post("/api/notice/notices/1/read-status")
                        .header("X-Login-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(com.officeflow.common.api.ResultCode.PARAM_ERROR.code()));
    }

    @Test
    @DisplayName("批量标记已读 - 成功")
    void batchRead_Success() throws Exception {
        BatchReadDTO dto = new BatchReadDTO();
        dto.setNoticeIds(List.of(1L, 2L, 3L));

        when(noticeService.batchRead(any(), any(), any())).thenReturn(3);

        mockMvc.perform(post("/api/notice/notices/batch-read")
                        .header("X-Login-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    @DisplayName("批量标记已读 - 验证失败")
    void batchRead_ValidationError() throws Exception {
        BatchReadDTO dto = new BatchReadDTO();
        dto.setNoticeIds(null);

        mockMvc.perform(post("/api/notice/notices/batch-read")
                        .header("X-Login-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(com.officeflow.common.api.ResultCode.PARAM_ERROR.code()));
    }

    @Test
    @DisplayName("获取未读数量 - 成功")
    void getUnreadCount_Success() throws Exception {
        UnreadCountVO vo = new UnreadCountVO();
        vo.setTotal(5L);
        Map<String, Long> byType = new HashMap<>();
        byType.put("COMPANY", 2L);
        vo.setByType(byType);
        vo.setByPriority(new HashMap<>());

        when(noticeService.getUnreadCount(any(), any(), any())).thenReturn(vo);

        mockMvc.perform(get("/api/notice/notices/unread-count")
                        .header("X-Login-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(5));
    }

    @Test
    @DisplayName("获取未读数量 - 带筛选条件")
    void getUnreadCount_WithFilters() throws Exception {
        UnreadCountVO vo = new UnreadCountVO();
        vo.setTotal(2L);
        vo.setByType(new HashMap<>());
        vo.setByPriority(new HashMap<>());

        when(noticeService.getUnreadCount(any(), any(), any())).thenReturn(vo);

        mockMvc.perform(get("/api/notice/notices/unread-count")
                        .header("X-Login-User-Id", "100")
                        .param("noticeType", "COMPANY")
                        .param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    @DisplayName("获取未读数量 - 用户未登录")
    void getUnreadCount_NotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/notice/notices/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(com.officeflow.common.api.ResultCode.FAIL.code()));
    }

    @Test
    @DisplayName("IP地址提取 - 从X-Forwarded-For获取")
    void getClientIp_FromXForwardedFor() throws Exception {
        NoticeDetailVO detailVO = new NoticeDetailVO();
        detailVO.setId(1L);

        when(noticeService.getNoticeDetail(any(), any(), any())).thenReturn(detailVO);

        mockMvc.perform(get("/api/notice/notices/1")
                        .header("X-Login-User-Id", "100")
                        .header("X-Forwarded-For", "192.168.1.1, 10.0.0.1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IP地址提取 - 从X-Real-IP获取")
    void getClientIp_FromXRealIp() throws Exception {
        NoticeDetailVO detailVO = new NoticeDetailVO();
        detailVO.setId(1L);

        when(noticeService.getNoticeDetail(any(), any(), any())).thenReturn(detailVO);

        mockMvc.perform(get("/api/notice/notices/1")
                        .header("X-Login-User-Id", "100")
                        .header("X-Real-IP", "192.168.1.2"))
                .andExpect(status().isOk());
    }
}

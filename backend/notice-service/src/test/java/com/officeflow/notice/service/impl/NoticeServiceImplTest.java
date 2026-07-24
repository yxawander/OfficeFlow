package com.officeflow.notice.service.impl;

import com.officeflow.api.user.client.UserAdminClient;
import com.officeflow.api.user.vo.DeptVO;
import com.officeflow.api.user.vo.UserOptionVO;
import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.api.PageResult;
import com.officeflow.common.exception.BusinessException;
import com.officeflow.notice.dto.*;
import com.officeflow.notice.entity.Notice;
import com.officeflow.notice.entity.NoticeRead;
import com.officeflow.notice.entity.NoticeScope;
import com.officeflow.notice.mapper.NoticeAttachmentMapper;
import com.officeflow.notice.mapper.NoticeMapper;
import com.officeflow.notice.mapper.NoticeReadMapper;
import com.officeflow.notice.mapper.NoticeScopeMapper;
import com.officeflow.notice.service.NoticeSearchService;
import com.officeflow.notice.vo.AdminNoticeListVO;
import com.officeflow.notice.vo.NoticeDetailVO;
import com.officeflow.notice.vo.NoticeListVO;
import com.officeflow.notice.vo.NoticeReadDetailVO;
import com.officeflow.notice.vo.UnreadCountVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("公告服务单元测试")
class NoticeServiceImplTest {

    @Mock
    private NoticeMapper noticeMapper;

    @Mock
    private NoticeScopeMapper noticeScopeMapper;

    @Mock
    private NoticeReadMapper noticeReadMapper;

    @Mock
    private UserAdminClient userAdminClient;

    @Mock
    private NoticeAttachmentMapper noticeAttachmentMapper;

    @Mock
    private NoticeSearchService noticeSearchService;

    @InjectMocks
    private NoticeServiceImpl noticeService;

    private NoticeCreateDTO createDTO;
    private NoticeUpdateDTO updateDTO;
    private NoticeDetailVO detailVO;

    @BeforeEach
    void setUp() {
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
        updateDTO.setTitle("更新标题");
        updateDTO.setContent("更新内容");
        updateDTO.setNoticeType("DEPT");
        updateDTO.setPriority("NORMAL");
        updateDTO.setExpireTime(LocalDateTime.now().plusDays(14));
        updateDTO.setScopes(List.of(scopeDTO));

        detailVO = new NoticeDetailVO();
        detailVO.setId(1L);
        detailVO.setTitle("测试公告");
        detailVO.setContent("测试内容");
        detailVO.setNoticeType("COMPANY");
        detailVO.setPriority("HIGH");
        detailVO.setPublisherId(100L);
        detailVO.setPublisherName("admin");
        detailVO.setPublishTime(LocalDateTime.now());
        detailVO.setExpireTime(LocalDateTime.now().plusDays(7));
        detailVO.setViewCount(10);
        detailVO.setReadCount(5);
        detailVO.setReadStatus(0);
    }

    @Test
    @DisplayName("获取公告列表 - 成功")
    void getNoticeList_Success() {
        NoticeQueryDTO dto = new NoticeQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);

        List<NoticeListVO> mockList = List.of(new NoticeListVO());
        when(noticeMapper.selectUserNoticeList(any(), anyLong(), anyLong(), any())).thenReturn(mockList);
        when(noticeMapper.countUserNoticeList(any(), anyLong(), anyLong(), any())).thenReturn(1L);

        PageResult<NoticeListVO> result = noticeService.getNoticeList(dto, 100L, 1L, List.of("EMPLOYEE"));

        assertThat(result).isNotNull();
        assertThat(result.records()).hasSize(1);
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.pageNum()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(10);
        verify(noticeMapper).selectUserNoticeList(any(), anyLong(), anyLong(), any());
        verify(noticeMapper).countUserNoticeList(any(), anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("获取公告详情 - 首次阅读")
    void getNoticeDetail_FirstRead() {
        detailVO.setReadStatus(0);
        when(noticeMapper.selectDetailById(1L, 100L)).thenReturn(detailVO);
        when(noticeMapper.incrementViewCount(1L)).thenReturn(1);
        when(noticeReadMapper.insert(any())).thenReturn(1);
        when(noticeMapper.incrementReadCount(1L)).thenReturn(1);

        NoticeDetailVO result = noticeService.getNoticeDetail(1L, 100L, "127.0.0.1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getReadStatus()).isEqualTo(1);
        assertThat(result.getReadAt()).isNotNull();

        verify(noticeMapper).incrementViewCount(1L);
        verify(noticeReadMapper).insert(any(NoticeRead.class));
        verify(noticeMapper).incrementReadCount(1L);
    }

    @Test
    @DisplayName("获取公告详情 - 已阅读")
    void getNoticeDetail_AlreadyRead() {
        detailVO.setReadStatus(1);
        detailVO.setReadAt(LocalDateTime.now().minusHours(1));
        when(noticeMapper.selectDetailById(1L, 100L)).thenReturn(detailVO);
        when(noticeMapper.incrementViewCount(1L)).thenReturn(1);

        NoticeDetailVO result = noticeService.getNoticeDetail(1L, 100L, "127.0.0.1");

        assertThat(result).isNotNull();
        assertThat(result.getReadStatus()).isEqualTo(1);
        assertThat(result.getReadAt()).isBefore(LocalDateTime.now());

        verify(noticeMapper).incrementViewCount(1L);
        verify(noticeReadMapper, never()).insert(any());
        verify(noticeMapper, never()).incrementReadCount(1L);
    }

    @Test
    @DisplayName("获取公告详情 - 公告不存在")
    void getNoticeDetail_NotExists() {
        when(noticeMapper.selectDetailById(999L, 100L)).thenReturn(null);

        assertThatThrownBy(() -> noticeService.getNoticeDetail(999L, 100L, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("公告不存在");
    }

    @Test
    @DisplayName("预览公告 - 成功")
    void previewNotice_Success() {
        when(noticeMapper.selectDetailById(1L, 100L)).thenReturn(detailVO);

        NoticeDetailVO result = noticeService.previewNotice(1L, 100L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        verify(noticeMapper).selectDetailById(1L, 100L);
        verify(noticeMapper, never()).incrementViewCount(any());
    }

    @Test
    @DisplayName("预览公告 - 公告不存在")
    void previewNotice_NotExists() {
        when(noticeMapper.selectDetailById(999L, 100L)).thenReturn(null);

        assertThatThrownBy(() -> noticeService.previewNotice(999L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("公告不存在");
    }

    @Test
    @DisplayName("设置阅读状态 - 成功")
    void setReadStatus_Success() {
        when(noticeReadMapper.updateReadStatus(1L, 100L, (byte) 1, "127.0.0.1")).thenReturn(1);

        NoticeReadStatusDTO dto = new NoticeReadStatusDTO();
        dto.setReadStatus(1);
        Boolean result = noticeService.setReadStatus(1L, dto, 100L, "127.0.0.1");

        assertThat(result).isTrue();
        verify(noticeReadMapper).updateReadStatus(1L, 100L, (byte) 1, "127.0.0.1");
    }

    @Test
    @DisplayName("批量标记已读 - 成功")
    void batchRead_Success() {
        BatchReadDTO dto = new BatchReadDTO();
        dto.setNoticeIds(Arrays.asList(1L, 2L, 3L));

        NoticeRead existingRead = new NoticeRead();
        existingRead.setReadStatus((byte) 0);

        when(noticeReadMapper.batchInsert(any())).thenReturn(3);
        when(noticeReadMapper.selectByNoticeIdAndUserId(anyLong(), anyLong())).thenReturn(existingRead);

        Integer result = noticeService.batchRead(dto, 100L, "127.0.0.1");

        assertThat(result).isEqualTo(3);
        verify(noticeReadMapper).batchInsert(any());
    }

    @Test
    @DisplayName("获取未读数量 - 无筛选条件")
    void getUnreadCount_NoFilters() {
        when(noticeReadMapper.countUnreadByUserId(100L)).thenReturn(5L);

        List<Map<String, Object>> byType = new ArrayList<>();
        Map<String, Object> typeItem1 = new HashMap<>();
        typeItem1.put("key", "COMPANY");
        typeItem1.put("value", 2L);
        byType.add(typeItem1);
        Map<String, Object> typeItem2 = new HashMap<>();
        typeItem2.put("key", "DEPT");
        typeItem2.put("value", 3L);
        byType.add(typeItem2);
        when(noticeReadMapper.countUnreadByType(100L)).thenReturn(byType);

        List<Map<String, Object>> byPriority = new ArrayList<>();
        Map<String, Object> priItem1 = new HashMap<>();
        priItem1.put("key", "HIGH");
        priItem1.put("value", 1L);
        byPriority.add(priItem1);
        Map<String, Object> priItem2 = new HashMap<>();
        priItem2.put("key", "NORMAL");
        priItem2.put("value", 4L);
        byPriority.add(priItem2);
        when(noticeReadMapper.countUnreadByPriority(100L)).thenReturn(byPriority);

        UnreadCountVO result = noticeService.getUnreadCount(100L, null, null);

        Map<String, Long> expectedByType = new HashMap<>();
        expectedByType.put("COMPANY", 2L);
        expectedByType.put("DEPT", 3L);
        Map<String, Long> expectedByPriority = new HashMap<>();
        expectedByPriority.put("HIGH", 1L);
        expectedByPriority.put("NORMAL", 4L);

        assertThat(result.getTotal()).isEqualTo(5L);
        assertThat(result.getByType()).isEqualTo(expectedByType);
        assertThat(result.getByPriority()).isEqualTo(expectedByPriority);

        verify(noticeReadMapper).countUnreadByUserId(100L);
        verify(noticeReadMapper).countUnreadByType(100L);
        verify(noticeReadMapper).countUnreadByPriority(100L);
    }

    @Test
    @DisplayName("获取未读数量 - 带筛选条件")
    void getUnreadCount_WithFilters() {
        when(noticeReadMapper.countUnreadByUserIdAndFilters(100L, "COMPANY", "HIGH")).thenReturn(2L);

        UnreadCountVO result = noticeService.getUnreadCount(100L, "COMPANY", "HIGH");

        assertThat(result.getTotal()).isEqualTo(2L);
        assertThat(result.getByType()).isEmpty();
        assertThat(result.getByPriority()).isEmpty();

        verify(noticeReadMapper).countUnreadByUserIdAndFilters(100L, "COMPANY", "HIGH");
        verify(noticeReadMapper, never()).countUnreadByType(any());
    }

    @Test
    @DisplayName("创建公告 - 成功")
    void createNotice_Success() {
        when(noticeMapper.insert(any(Notice.class))).thenAnswer(invocation -> {
            Notice notice = invocation.getArgument(0);
            notice.setId(1L);
            return 1;
        });
        when(noticeScopeMapper.insert(any(NoticeScope.class))).thenReturn(1);

        Long result = noticeService.createNotice(createDTO, 100L, "admin");

        assertThat(result).isEqualTo(1L);
        verify(noticeMapper).insert(any(Notice.class));
        verify(noticeScopeMapper).insert(any(NoticeScope.class));
    }

    @Test
    @DisplayName("更新公告 - 成功")
    void updateNotice_Success() {
        Notice existingNotice = new Notice();
        existingNotice.setId(1L);
        existingNotice.setStatus("DRAFT");

        when(noticeMapper.selectById(1L)).thenReturn(existingNotice);
        when(noticeMapper.updateById(any(Notice.class))).thenReturn(1);
        when(noticeScopeMapper.deleteByNoticeId(1L)).thenReturn(1);
        when(noticeScopeMapper.insert(any(NoticeScope.class))).thenReturn(1);

        Boolean result = noticeService.updateNotice(1L, updateDTO);

        assertThat(result).isTrue();
        verify(noticeMapper, times(2)).selectById(1L);
        verify(noticeMapper).updateById(any(Notice.class));
        verify(noticeScopeMapper).deleteByNoticeId(1L);
        verify(noticeScopeMapper).insert(any(NoticeScope.class));
    }

    @Test
    @DisplayName("更新公告 - 公告不存在")
    void updateNotice_NotExists() {
        when(noticeMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> noticeService.updateNotice(999L, updateDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessage("公告不存在");
    }

    @Test
    @DisplayName("更新公告 - 状态不允许")
    void updateNotice_StatusNotAllowed() {
        Notice existingNotice = new Notice();
        existingNotice.setId(1L);
        existingNotice.setStatus("PUBLISHED");

        when(noticeMapper.selectById(1L)).thenReturn(existingNotice);

        assertThatThrownBy(() -> noticeService.updateNotice(1L, updateDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessage("仅草稿或已下线状态可编辑");
    }

    @Test
    @DisplayName("发布公告 - 成功")
    void publishNotice_Success() {
        Notice existingNotice = new Notice();
        existingNotice.setId(1L);
        existingNotice.setStatus("DRAFT");

        when(noticeMapper.selectById(1L)).thenReturn(existingNotice);
        when(noticeMapper.updateStatusById(1L, "PUBLISHED")).thenReturn(1);

        Boolean result = noticeService.publishNotice(1L);

        assertThat(result).isTrue();
        verify(noticeMapper, times(2)).selectById(1L);
        verify(noticeMapper).updateStatusById(1L, "PUBLISHED");
    }

    @Test
    @DisplayName("发布公告 - 公告不存在")
    void publishNotice_NotExists() {
        when(noticeMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> noticeService.publishNotice(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("公告不存在");
    }

    @Test
    @DisplayName("发布公告 - 状态不允许")
    void publishNotice_StatusNotAllowed() {
        Notice existingNotice = new Notice();
        existingNotice.setId(1L);
        existingNotice.setStatus("PUBLISHED");

        when(noticeMapper.selectById(1L)).thenReturn(existingNotice);

        assertThatThrownBy(() -> noticeService.publishNotice(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("仅草稿或已下线状态可发布");
    }

    @Test
    @DisplayName("下线公告 - 成功")
    void offlineNotice_Success() {
        Notice existingNotice = new Notice();
        existingNotice.setId(1L);
        existingNotice.setStatus("PUBLISHED");

        when(noticeMapper.selectById(1L)).thenReturn(existingNotice);
        when(noticeMapper.updateStatusById(1L, "OFFLINE")).thenReturn(1);

        Boolean result = noticeService.offlineNotice(1L);

        assertThat(result).isTrue();
        verify(noticeMapper, times(2)).selectById(1L);
        verify(noticeMapper).updateStatusById(1L, "OFFLINE");
    }

    @Test
    @DisplayName("下线公告 - 状态不允许")
    void offlineNotice_StatusNotAllowed() {
        Notice existingNotice = new Notice();
        existingNotice.setId(1L);
        existingNotice.setStatus("DRAFT");

        when(noticeMapper.selectById(1L)).thenReturn(existingNotice);

        assertThatThrownBy(() -> noticeService.offlineNotice(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("仅已发布状态可下线");
    }

    @Test
    @DisplayName("删除公告 - 成功")
    void deleteNotice_Success() {
        Notice existingNotice = new Notice();
        existingNotice.setId(1L);
        existingNotice.setStatus("DRAFT");

        when(noticeMapper.selectById(1L)).thenReturn(existingNotice);
        when(noticeMapper.deleteById(1L)).thenReturn(1);
        when(noticeScopeMapper.deleteByNoticeId(1L)).thenReturn(1);

        Boolean result = noticeService.deleteNotice(1L);

        assertThat(result).isTrue();
        verify(noticeMapper).selectById(1L);
        verify(noticeMapper).deleteById(1L);
        verify(noticeScopeMapper).deleteByNoticeId(1L);
    }

    @Test
    @DisplayName("删除公告 - 状态不允许")
    void deleteNotice_StatusNotAllowed() {
        Notice existingNotice = new Notice();
        existingNotice.setId(1L);
        existingNotice.setStatus("PUBLISHED");

        when(noticeMapper.selectById(1L)).thenReturn(existingNotice);

        assertThatThrownBy(() -> noticeService.deleteNotice(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("仅草稿或已下线状态可删除");
    }

    @Test
    @DisplayName("获取管理端公告列表 - 成功")
    void getAdminNoticeList_Success() {
        NoticeQueryDTO dto = new NoticeQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);

        when(userAdminClient.getUserPage(null, null, 1, 1, 1))
                .thenReturn(ApiResponse.ok(PageResult.of(100L, 1L, 1L, List.of())));

        List<AdminNoticeListVO> mockList = List.of(new AdminNoticeListVO());
        when(noticeMapper.selectAdminNoticeList(any(), anyLong())).thenReturn(mockList);
        when(noticeMapper.countAdminNoticeList(any())).thenReturn(1L);

        PageResult<AdminNoticeListVO> result = noticeService.getAdminNoticeList(dto);

        assertThat(result).isNotNull();
        assertThat(result.records()).hasSize(1);
        assertThat(result.total()).isEqualTo(1);

        verify(noticeMapper).selectAdminNoticeList(any(), anyLong());
        verify(noticeMapper).countAdminNoticeList(any());
    }

    @Test
    @DisplayName("获取公告阅读详情 - 成功")
    void getNoticeReadDetail_Success() {
        NoticeReadDetailVO detailVO = new NoticeReadDetailVO();
        detailVO.setNoticeId(1L);

        when(userAdminClient.getUserPage(null, null, 1, 1, 1))
                .thenReturn(ApiResponse.ok(PageResult.of(100L, 1L, 1L, List.of())));

        DeptVO dept = new DeptVO();
        dept.setId(1L);
        dept.setDeptName("技术部");
        when(userAdminClient.getDeptList()).thenReturn(ApiResponse.ok(List.of(dept)));

        UserOptionVO user = new UserOptionVO();
        user.setId(100L);
        user.setDeptId(1L);
        when(userAdminClient.getUserOptions()).thenReturn(ApiResponse.ok(List.of(user)));

        when(noticeReadMapper.selectReadDetailById(eq(1L), anyLong())).thenReturn(detailVO);
        when(noticeReadMapper.selectReadUserIdsByNoticeId(1L)).thenReturn(List.of());

        NoticeReadDetailVO result = noticeService.getNoticeReadDetail(1L);

        assertThat(result).isNotNull();
        assertThat(result.getNoticeId()).isEqualTo(1L);
        assertThat(result.getDeptStats()).isNotNull();

        verify(noticeReadMapper).selectReadDetailById(eq(1L), anyLong());
        verify(noticeReadMapper).selectReadUserIdsByNoticeId(1L);
    }

    @Test
    @DisplayName("获取公告阅读详情 - 公告不存在")
    void getNoticeReadDetail_NotExists() {
        when(userAdminClient.getUserPage(null, null, 1, 1, 1))
                .thenReturn(ApiResponse.ok(PageResult.of(100L, 1L, 1L, List.of())));

        when(noticeReadMapper.selectReadDetailById(eq(999L), anyLong())).thenReturn(null);

        assertThatThrownBy(() -> noticeService.getNoticeReadDetail(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("公告不存在");
    }
}
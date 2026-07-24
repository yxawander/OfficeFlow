package com.officeflow.notice.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.officeflow.common.api.PageResult;
import com.officeflow.notice.document.NoticeDocument;
import com.officeflow.notice.dto.NoticeQueryDTO;
import com.officeflow.notice.mapper.NoticeMapper;
import com.officeflow.notice.repository.NoticeSearchRepository;
import com.officeflow.notice.service.NoticeSearchService;
import com.officeflow.notice.vo.AdminNoticeListVO;
import com.officeflow.notice.vo.NoticeListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeSearchServiceImpl implements NoticeSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final NoticeSearchRepository searchRepository;
    private final NoticeMapper noticeMapper;

    @Override
    public PageResult<NoticeListVO> searchUser(String keyword, NoticeQueryDTO dto, Long userId, Long deptId) {
        try {
            return searchUserWithES(keyword, dto, userId, deptId);
        } catch (Exception e) {
            log.warn("ES search failed, falling back to MySQL: {}", e.getMessage());
            return searchUserWithMySQL(keyword, dto, userId, deptId);
        }
    }

    @Override
    public PageResult<AdminNoticeListVO> searchAdmin(String keyword, NoticeQueryDTO dto) {
        try {
            return searchAdminWithES(keyword, dto);
        } catch (Exception e) {
            log.warn("ES admin search failed, falling back to MySQL: {}", e.getMessage());
            return searchAdminWithMySQL(keyword, dto);
        }
    }

    @Async
    @Override
    public void indexAsync(NoticeDocument doc) {
        try {
            doc.setContent(NoticeDocument.stripHtml(doc.getContent()));
            searchRepository.save(doc);
        } catch (Exception e) {
            log.error("Failed to index notice document: id={}", doc.getId(), e);
        }
    }

    @Async
    @Override
    public void deleteAsync(Long id) {
        try {
            searchRepository.deleteById(id);
        } catch (Exception e) {
            log.error("Failed to delete notice document: id={}", id, e);
        }
    }

    @Override
    public int syncAll() {
        try {
            List<NoticeDocument> all = noticeMapper.selectAllForEs();
            if (all.isEmpty()) {
                return 0;
            }
            all.forEach(doc -> doc.setContent(NoticeDocument.stripHtml(doc.getContent())));
            searchRepository.saveAll(all);
            log.info("ES full sync completed: {} notice documents indexed", all.size());
            return all.size();
        } catch (Exception e) {
            log.error("ES full sync failed: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public int repairRecent(int minutes) {
        try {
            List<NoticeDocument> recent = noticeMapper.selectRecentForEs(minutes);
            if (recent.isEmpty()) {
                return 0;
            }
            recent.forEach(doc -> doc.setContent(NoticeDocument.stripHtml(doc.getContent())));
            searchRepository.saveAll(recent);
            log.info("ES repair: synced {} notice documents (last {} minutes)", recent.size(), minutes);
            return recent.size();
        } catch (Exception e) {
            log.error("ES repair failed: {}", e.getMessage());
            return 0;
        }
    }

    private PageResult<NoticeListVO> searchUserWithES(String keyword, NoticeQueryDTO dto, Long userId, Long deptId) {
        int pageNum = dto.getPageNum() != null ? dto.getPageNum() : 1;
        int pageSize = dto.getPageSize() != null ? dto.getPageSize() : 10;

        List<Query> must = new ArrayList<>();
        List<Query> filter = new ArrayList<>();

        if (StringUtils.hasText(keyword)) {
            must.add(Query.of(q -> q.multiMatch(mm -> mm
                    .fields("title", "content")
                    .query(keyword)
                    .type(TextQueryType.BestFields))));
        }

        filter.add(Query.of(q -> q.term(t -> t.field("isDeleted").value(0))));
        filter.add(Query.of(q -> q.term(t -> t.field("status").value("PUBLISHED"))));

        if (StringUtils.hasText(dto.getNoticeType())) {
            filter.add(Query.of(q -> q.term(t -> t.field("noticeType").value(dto.getNoticeType()))));
        }
        if (StringUtils.hasText(dto.getPriority())) {
            filter.add(Query.of(q -> q.term(t -> t.field("priority").value(dto.getPriority()))));
        }

        // Scope filter
        List<FieldValue> scopeValues = new ArrayList<>();
        scopeValues.add(FieldValue.of("ALL"));
        scopeValues.add(FieldValue.of("U:" + userId));
        if (deptId != null) {
            scopeValues.add(FieldValue.of("D:" + deptId));
        }
        filter.add(Query.of(q -> q.terms(t -> t.field("scopeKeys")
                .terms(tt -> tt.value(scopeValues)))));

        BoolQuery boolQuery = BoolQuery.of(b -> b.must(must).filter(filter));
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQuery))
                .withPageable(PageRequest.of(pageNum - 1, pageSize))
                .build();

        SearchHits<NoticeDocument> hits = elasticsearchOperations.search(nativeQuery, NoticeDocument.class);

        List<NoticeListVO> records = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toListVO)
                .collect(Collectors.toList());

        return new PageResult<>(hits.getTotalHits(), pageNum, pageSize, records);
    }

    private PageResult<AdminNoticeListVO> searchAdminWithES(String keyword, NoticeQueryDTO dto) {
        int pageNum = dto.getPageNum() != null ? dto.getPageNum() : 1;
        int pageSize = dto.getPageSize() != null ? dto.getPageSize() : 10;

        List<Query> must = new ArrayList<>();
        List<Query> filter = new ArrayList<>();

        if (StringUtils.hasText(keyword)) {
            must.add(Query.of(q -> q.multiMatch(mm -> mm
                    .fields("title", "content")
                    .query(keyword)
                    .type(TextQueryType.BestFields))));
        }

        filter.add(Query.of(q -> q.term(t -> t.field("isDeleted").value(0))));

        if (StringUtils.hasText(dto.getStatus())) {
            filter.add(Query.of(q -> q.term(t -> t.field("status").value(dto.getStatus()))));
        }
        if (dto.getPublisherId() != null) {
            filter.add(Query.of(q -> q.term(t -> t.field("publisherId").value(dto.getPublisherId()))));
        }

        BoolQuery boolQuery = BoolQuery.of(b -> b.must(must).filter(filter));
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQuery))
                .withPageable(PageRequest.of(pageNum - 1, pageSize))
                .build();

        SearchHits<NoticeDocument> hits = elasticsearchOperations.search(nativeQuery, NoticeDocument.class);

        List<AdminNoticeListVO> records = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toAdminVO)
                .collect(Collectors.toList());

        return new PageResult<>(hits.getTotalHits(), pageNum, pageSize, records);
    }

    private PageResult<NoticeListVO> searchUserWithMySQL(String keyword, NoticeQueryDTO dto, Long userId, Long deptId) {
        if (StringUtils.hasText(keyword)) {
            dto.setKeyword(keyword);
        }
        dto.setOnlyPublished(true);
        List<NoticeListVO> records = noticeMapper.selectUserNoticeList(dto, userId, deptId, null);
        long total = noticeMapper.countUserNoticeList(dto, userId, deptId, null);
        return new PageResult<>(total,
                dto.getPageNum() != null ? dto.getPageNum() : 1,
                dto.getPageSize() != null ? dto.getPageSize() : 10,
                records);
    }

    private PageResult<AdminNoticeListVO> searchAdminWithMySQL(String keyword, NoticeQueryDTO dto) {
        if (StringUtils.hasText(keyword)) {
            dto.setKeyword(keyword);
        }
        List<AdminNoticeListVO> records = noticeMapper.selectAdminNoticeList(dto, 1L);
        long total = noticeMapper.countAdminNoticeList(dto);
        return new PageResult<>(total,
                dto.getPageNum() != null ? dto.getPageNum() : 1,
                dto.getPageSize() != null ? dto.getPageSize() : 10,
                records);
    }

    private NoticeListVO toListVO(NoticeDocument doc) {
        NoticeListVO vo = new NoticeListVO();
        vo.setId(doc.getId());
        vo.setTitle(doc.getTitle());
        String plain = NoticeDocument.stripHtml(doc.getContent());
        vo.setSummary(plain != null && plain.length() > 100 ? plain.substring(0, 100) : plain);
        vo.setNoticeType(doc.getNoticeType());
        vo.setPriority(doc.getPriority());
        vo.setPublisherId(doc.getPublisherId());
        vo.setPublisherName(doc.getPublisherName());
        vo.setPublishTime(doc.getPublishTime());
        vo.setExpireTime(doc.getExpireTime());
        vo.setReadStatus(0);
        return vo;
    }

    private AdminNoticeListVO toAdminVO(NoticeDocument doc) {
        AdminNoticeListVO vo = new AdminNoticeListVO();
        vo.setId(doc.getId());
        vo.setTitle(doc.getTitle());
        vo.setNoticeType(doc.getNoticeType());
        vo.setPriority(doc.getPriority());
        vo.setPublisherId(doc.getPublisherId());
        vo.setPublisherName(doc.getPublisherName());
        vo.setPublishTime(doc.getPublishTime());
        vo.setStatus(doc.getStatus());
        vo.setReadCount(doc.getReadCount());
        vo.setViewCount(doc.getViewCount());
        vo.setCreatedAt(doc.getCreatedAt());
        return vo;
    }
}

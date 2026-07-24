package com.officeflow.flow.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.officeflow.common.api.PageResult;
import com.officeflow.flow.document.FlowApplyDocument;
import com.officeflow.flow.dto.FlowApplyQueryDTO;
import com.officeflow.flow.mapper.FlowApplyMapper;
import com.officeflow.flow.repository.FlowApplySearchRepository;
import com.officeflow.flow.service.FlowSearchService;
import com.officeflow.flow.vo.FlowApplyListVO;
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
public class FlowSearchServiceImpl implements FlowSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final FlowApplySearchRepository searchRepository;
    private final FlowApplyMapper flowApplyMapper;

    @Override
    public PageResult<FlowApplyListVO> search(String keyword, FlowApplyQueryDTO queryDTO, Long userId, Long deptId) {
        try {
            return searchWithES(keyword, queryDTO, userId, deptId);
        } catch (Exception e) {
            log.warn("ES search failed, falling back to MySQL: {}", e.getMessage());
            return searchWithMySQL(keyword, queryDTO, userId);
        }
    }

    @Async
    @Override
    public void indexAsync(FlowApplyDocument doc) {
        try {
            searchRepository.save(doc);
        } catch (Exception e) {
            log.error("Failed to index flow_apply document: id={}", doc.getId(), e);
        }
    }

    @Async
    @Override
    public void deleteAsync(Long id) {
        try {
            searchRepository.deleteById(id);
        } catch (Exception e) {
            log.error("Failed to delete flow_apply document: id={}", id, e);
        }
    }

    @Override
    public int syncAll() {
        try {
            List<FlowApplyDocument> all = flowApplyMapper.selectAllForEs();
            if (all.isEmpty()) {
                return 0;
            }
            searchRepository.saveAll(all);
            log.info("ES full sync completed: {} flow_apply documents indexed", all.size());
            return all.size();
        } catch (Exception e) {
            log.error("ES full sync failed: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public int repairRecent(int minutes) {
        try {
            // 从 MySQL 获取最近变更的审批单并全量同步
            List<FlowApplyDocument> recent = flowApplyMapper.selectRecentForEs(minutes);
            if (recent.isEmpty()) {
                return 0;
            }
            searchRepository.saveAll(recent);
            log.info("ES repair: synced {} flow_apply documents (last {} minutes)", recent.size(), minutes);
            return recent.size();
        } catch (Exception e) {
            log.error("ES repair failed: {}", e.getMessage());
            return 0;
        }
    }

    private PageResult<FlowApplyListVO> searchWithES(String keyword, FlowApplyQueryDTO queryDTO, Long userId, Long deptId) {
        int pageNum = queryDTO.getPageNum() != null ? queryDTO.getPageNum() : 1;
        int pageSize = queryDTO.getPageSize() != null ? queryDTO.getPageSize() : 10;

        List<Query> must = new ArrayList<>();
        List<Query> filter = new ArrayList<>();

        // 关键词搜索 title + reason
        if (StringUtils.hasText(keyword)) {
            must.add(Query.of(q -> q.multiMatch(mm -> mm
                    .fields("title", "reason")
                    .query(keyword)
                    .type(TextQueryType.BestFields))));
        } else {
            // 无关键词时用 matchAll 确保纯过滤查询正确执行
            must.add(Query.of(q -> q.matchAll(m -> m)));
        }

        // 过滤已删除
        filter.add(Query.of(q -> q.term(t -> t.field("isDeleted").value(0))));

        // 过滤申请人（我的申请）
        if (userId != null) {
            filter.add(Query.of(q -> q.term(t -> t.field("applicantId").value(userId))));
        }

        // 类型过滤
        if (StringUtils.hasText(queryDTO.getApplyType())) {
            filter.add(Query.of(q -> q.term(t -> t.field("applyType").value(queryDTO.getApplyType()))));
        }

        // 状态过滤
        if (StringUtils.hasText(queryDTO.getStatus())) {
            filter.add(Query.of(q -> q.term(t -> t.field("status").value(queryDTO.getStatus()))));
        }

        BoolQuery boolQuery = BoolQuery.of(b -> b.must(must).filter(filter));
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQuery))
                .withPageable(PageRequest.of(pageNum - 1, pageSize))
                .build();

        SearchHits<FlowApplyDocument> hits = elasticsearchOperations.search(nativeQuery, FlowApplyDocument.class);

        List<FlowApplyListVO> records = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toListVO)
                .collect(Collectors.toList());

        return new PageResult<>(hits.getTotalHits(), pageNum, pageSize, records);
    }

    private PageResult<FlowApplyListVO> searchWithMySQL(String keyword, FlowApplyQueryDTO queryDTO, Long userId) {
        if (StringUtils.hasText(keyword)) {
            queryDTO.setKeyword(keyword);
        }
        List<FlowApplyListVO> records = flowApplyMapper.selectUserApplies(queryDTO, userId);
        long total = flowApplyMapper.countUserApplies(queryDTO, userId);
        return new PageResult<>(total,
                queryDTO.getPageNum() != null ? queryDTO.getPageNum() : 1,
                queryDTO.getPageSize() != null ? queryDTO.getPageSize() : 10,
                records);
    }

    private FlowApplyListVO toListVO(FlowApplyDocument doc) {
        FlowApplyListVO vo = new FlowApplyListVO();
        vo.setId(doc.getId());
        vo.setApplyNo(doc.getApplyNo());
        vo.setApplyType(doc.getApplyType());
        vo.setTitle(doc.getTitle());
        vo.setReason(doc.getReason());
        vo.setDurationHours(doc.getDurationHours());
        vo.setStatus(doc.getStatus());
        vo.setApproverName(doc.getApproverName());
        vo.setStartTime(doc.getStartTime());
        vo.setEndTime(doc.getEndTime());
        vo.setCreatedAt(doc.getCreatedAt());
        return vo;
    }
}

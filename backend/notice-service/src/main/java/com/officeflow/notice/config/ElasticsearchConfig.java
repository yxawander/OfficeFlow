package com.officeflow.notice.config;

import com.officeflow.notice.document.NoticeDocument;
import com.officeflow.notice.service.NoticeSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

@Slf4j
@Configuration
public class ElasticsearchConfig {

    private final ElasticsearchOperations elasticsearchOperations;
    private final NoticeSearchService noticeSearchService;

    public ElasticsearchConfig(ElasticsearchOperations elasticsearchOperations,
                               NoticeSearchService noticeSearchService) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.noticeSearchService = noticeSearchService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(NoticeDocument.class);
            if (!indexOps.exists()) {
                indexOps.create();
                indexOps.putMapping(indexOps.createMapping());
                log.info("Elasticsearch index 'notice' created successfully");
            }
            int count = noticeSearchService.syncAll();
            log.info("ES sync on startup completed: {} documents", count);
        } catch (Exception e) {
            log.warn("Elasticsearch unavailable, search will fallback to MySQL: {}", e.getMessage());
        }
    }
}

package com.officeflow.flow.config;

import com.officeflow.flow.service.FlowSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;

@Slf4j
@Configuration
public class ElasticsearchConfig {

    private final ElasticsearchOperations elasticsearchOperations;
    private final FlowSearchService flowSearchService;

    public ElasticsearchConfig(ElasticsearchOperations elasticsearchOperations,
                               FlowSearchService flowSearchService) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.flowSearchService = flowSearchService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(
                    com.officeflow.flow.document.FlowApplyDocument.class);
            if (!indexOps.exists()) {
                indexOps.create();
                Document mapping = indexOps.createMapping();
                indexOps.putMapping(mapping);
                log.info("Elasticsearch index 'flow_apply' created successfully");
            }
            // 每次启动全量同步（saveAll 按 ID upsert，不会重复）
            int count = flowSearchService.syncAll();
            log.info("ES sync on startup completed: {} documents", count);
        } catch (Exception e) {
            log.warn("Elasticsearch unavailable, search will fallback to MySQL: {}", e.getMessage());
        }
    }
}

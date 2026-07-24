package com.officeflow.notice.repository;

import com.officeflow.notice.document.NoticeDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeSearchRepository extends ElasticsearchRepository<NoticeDocument, Long> {
}

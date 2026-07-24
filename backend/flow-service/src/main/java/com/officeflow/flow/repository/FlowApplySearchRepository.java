package com.officeflow.flow.repository;

import com.officeflow.flow.document.FlowApplyDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlowApplySearchRepository extends ElasticsearchRepository<FlowApplyDocument, Long> {

    List<FlowApplyDocument> findByApplicantIdAndIsDeleted(Long applicantId, Integer isDeleted);

    List<FlowApplyDocument> findByApproverIdAndIsDeleted(Long approverId, Integer isDeleted);
}

package com.officeflow.flow.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.officeflow.flow.entity.FlowApply;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "flow_apply")
@Setting(settingPath = "es/flow-apply-setting.json")
public class FlowApplyDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String applyNo;

    @Field(type = FieldType.Text, analyzer = "ik_max_word_analyzer", searchAnalyzer = "ik_smart_analyzer")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word_analyzer", searchAnalyzer = "ik_smart_analyzer")
    private String reason;

    @Field(type = FieldType.Keyword)
    private String applyType;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Long)
    private Long applicantId;

    @Field(type = FieldType.Keyword)
    private String applicantName;

    @Field(type = FieldType.Long)
    private Long applicantDeptId;

    @Field(type = FieldType.Keyword)
    private String applicantDeptName;

    @Field(type = FieldType.Long)
    private Long approverId;

    @Field(type = FieldType.Keyword)
    private String approverName;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Field(type = FieldType.Double)
    private BigDecimal durationHours;

    @Field(type = FieldType.Integer)
    private Integer isDeleted;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public static FlowApplyDocument from(FlowApply apply, String applicantName,
                                          String applicantDeptName, String approverName) {
        return FlowApplyDocument.builder()
                .id(apply.getId())
                .applyNo(apply.getApplyNo())
                .title(apply.getTitle())
                .reason(apply.getReason())
                .applyType(apply.getApplyType())
                .status(apply.getStatus())
                .applicantId(apply.getApplicantId())
                .applicantName(applicantName)
                .applicantDeptId(apply.getApplicantDeptId())
                .applicantDeptName(applicantDeptName)
                .approverId(apply.getApproverId())
                .approverName(approverName)
                .startTime(apply.getStartTime())
                .endTime(apply.getEndTime())
                .durationHours(apply.getDurationHours())
                .isDeleted(apply.getIsDeleted() != null ? apply.getIsDeleted().intValue() : 0)
                .createdAt(apply.getCreatedAt())
                .build();
    }
}

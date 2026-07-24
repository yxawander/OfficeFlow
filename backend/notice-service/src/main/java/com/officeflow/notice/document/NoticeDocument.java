package com.officeflow.notice.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "notice")
@Setting(settingPath = "es/notice-setting.json")
public class NoticeDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word_analyzer", searchAnalyzer = "ik_smart_analyzer")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word_analyzer", searchAnalyzer = "ik_smart_analyzer")
    private String content;

    @Field(type = FieldType.Keyword)
    private String noticeType;

    @Field(type = FieldType.Keyword)
    private String priority;

    @Field(type = FieldType.Long)
    private Long publisherId;

    @Field(type = FieldType.Keyword)
    private String publisherName;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    @Field(type = FieldType.Integer)
    private Integer readCount;

    @Field(type = FieldType.Integer)
    private Integer viewCount;

    @Field(type = FieldType.Integer)
    private Integer isDeleted;

    @Field(type = FieldType.Keyword)
    private List<String> scopeKeys;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Transient
    private String scopeKeysStr;

    public void setScopeKeysStr(String scopeKeysStr) {
        this.scopeKeysStr = scopeKeysStr;
        if (scopeKeysStr != null && !scopeKeysStr.isEmpty()) {
            this.scopeKeys = Arrays.asList(scopeKeysStr.split(","));
        } else {
            this.scopeKeys = Collections.emptyList();
        }
    }

    public static String stripHtml(String html) {
        if (html == null) {
            return null;
        }
        return html.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
    }
}

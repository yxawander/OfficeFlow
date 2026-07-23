package com.officeflow.api.notice.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchReadDTO {
    @NotEmpty(message = "公告ID列表不能为空")
    private List<Long> noticeIds;
}
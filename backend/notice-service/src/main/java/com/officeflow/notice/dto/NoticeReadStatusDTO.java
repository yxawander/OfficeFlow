package com.officeflow.notice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NoticeReadStatusDTO {
    @NotNull(message = "阅读状态不能为空")
    private Integer readStatus;
}
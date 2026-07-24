package com.officeflow.api.notice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NoticeUpdateDTO extends NoticeCreateDTO {
    @NotNull(message = "公告ID不能为空")
    private Long id;
}
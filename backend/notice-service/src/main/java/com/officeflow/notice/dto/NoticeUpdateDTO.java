package com.officeflow.notice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoticeUpdateDTO extends NoticeCreateDTO {
    @NotBlank(message = "公告ID不能为空")
    private String id;
}
package com.officeflow.api.user.vo;

import lombok.Data;

@Data
public class PostVO {
    private Long id;
    private String postName;
    private String postCode;
    private Integer sortOrder;
    private Integer status;
}

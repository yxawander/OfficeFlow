package com.officeflow.api.user.vo;

import lombok.Data;

@Data
public class MenuVO {
    private Long id;
    private Long parentId;
    private String menuName;
    private String menuType;
    private String path;
    private String component;
    private String permission;
    private String icon;
    private Boolean visible;
    private Integer sortOrder;
    private Integer status;
}

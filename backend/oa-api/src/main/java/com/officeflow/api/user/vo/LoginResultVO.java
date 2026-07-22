package com.officeflow.api.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class LoginResultVO {
    private String token;
    private UserProfileVO profile;
    private List<RoleVO> roles;
    private List<String> permissions;
    private List<MenuVO> menus;
    private Long expireSeconds;
}

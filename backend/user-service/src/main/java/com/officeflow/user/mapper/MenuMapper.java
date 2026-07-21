package com.officeflow.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface MenuMapper {
    @Select("""
            SELECT id, parent_id AS parentId, menu_name AS menuName, menu_type AS menuType, path, component,
                   permission, icon, visible, sort_order AS sortOrder, status
            FROM sys_menu
            WHERE is_deleted = 0
            ORDER BY parent_id ASC, sort_order ASC, id ASC
            """)
    List<Map<String, Object>> listAll();

    @Select("""
            SELECT DISTINCT m.id, m.parent_id AS parentId, m.menu_name AS menuName, m.menu_type AS menuType,
                   m.path, m.component, m.permission, m.icon, m.visible, m.sort_order AS sortOrder, m.status
            FROM sys_menu m
            INNER JOIN sys_role_menu rm ON rm.menu_id = m.id
            INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id
            WHERE ur.user_id = #{userId} AND m.status = 1 AND m.is_deleted = 0
            ORDER BY m.parent_id ASC, m.sort_order ASC, m.id ASC
            """)
    List<Map<String, Object>> listByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT m.permission
            FROM sys_menu m
            INNER JOIN sys_role_menu rm ON rm.menu_id = m.id
            INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id
            WHERE ur.user_id = #{userId} AND m.permission IS NOT NULL AND m.permission <> ''
              AND m.status = 1 AND m.is_deleted = 0
            """)
    List<String> listPermissionsByUserId(@Param("userId") Long userId);
}

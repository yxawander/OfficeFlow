package com.officeflow.user.mapper;

import com.officeflow.user.dto.RoleRequest;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface RoleMapper {
    @Select("""
            SELECT id, role_name AS roleName, role_code AS roleCode, data_scope AS dataScope,
                   sort_order AS sortOrder, status
            FROM sys_role
            WHERE is_deleted = 0
            ORDER BY sort_order ASC, id ASC
            """)
    List<Map<String, Object>> listAll();

    @Select("""
            SELECT r.id, r.role_name AS roleName, r.role_code AS roleCode, r.data_scope AS dataScope
            FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.status = 1 AND r.is_deleted = 0
            ORDER BY r.sort_order ASC, r.id ASC
            """)
    List<Map<String, Object>> listByUserId(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO sys_role (role_name, role_code, data_scope, sort_order, status)
            VALUES (#{req.roleName}, #{req.roleCode}, #{dataScope}, #{sortOrder}, #{status})
            """)
    int insert(@Param("req") RoleRequest request, @Param("dataScope") String dataScope,
               @Param("sortOrder") Integer sortOrder, @Param("status") Integer status);

    @Update("""
            UPDATE sys_role
            SET role_name = #{req.roleName}, role_code = #{req.roleCode}, data_scope = #{dataScope},
                sort_order = #{sortOrder}, status = #{status}
            WHERE id = #{id} AND is_deleted = 0
            """)
    int update(@Param("id") Long id, @Param("req") RoleRequest request, @Param("dataScope") String dataScope,
               @Param("sortOrder") Integer sortOrder, @Param("status") Integer status);

    @Update("UPDATE sys_role SET is_deleted = 1 WHERE id = #{id}")
    int softDelete(@Param("id") Long id);

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> listRoleIdsByUserId(@Param("userId") Long userId);

    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> listMenuIdsByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT api_permission_id FROM sys_role_api_permission WHERE role_id = #{roleId}")
    List<Long> listApiPermissionIdsByRoleId(@Param("roleId") Long roleId);

    @Select("""
            SELECT DISTINCT rap.api_permission_id
            FROM sys_user_role ur
            INNER JOIN sys_role r ON r.id = ur.role_id AND r.status = 1 AND r.is_deleted = 0
            INNER JOIN sys_role_api_permission rap ON rap.role_id = r.id
            WHERE ur.user_id = #{userId}
            """)
    List<Long> listApiPermissionIdsByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteRoleMenus(@Param("roleId") Long roleId);

    @Insert("""
            <script>
            INSERT INTO sys_role_menu (role_id, menu_id) VALUES
            <foreach collection="menuIds" item="menuId" separator=",">
              (#{roleId}, #{menuId})
            </foreach>
            </script>
            """)
    int insertRoleMenus(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);

    @Delete("DELETE FROM sys_role_api_permission WHERE role_id = #{roleId}")
    int deleteRoleApiPermissions(@Param("roleId") Long roleId);

    @Insert("""
            <script>
            INSERT INTO sys_role_api_permission (role_id, api_permission_id) VALUES
            <foreach collection="permissionIds" item="permissionId" separator=",">
              (#{roleId}, #{permissionId})
            </foreach>
            </script>
            """)
    int insertRoleApiPermissions(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);
}

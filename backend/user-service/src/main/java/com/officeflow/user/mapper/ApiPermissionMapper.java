package com.officeflow.user.mapper;

import com.officeflow.user.dto.ApiPermissionRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ApiPermissionMapper {
    @Select("""
            SELECT id, permission_name AS permissionName, permission_code AS permissionCode,
                   service_name AS serviceName, request_method AS requestMethod, request_path AS requestPath, status
            FROM sys_api_permission
            ORDER BY service_name ASC, id ASC
            """)
    List<Map<String, Object>> listAll();

    @Insert("""
            INSERT INTO sys_api_permission (permission_name, permission_code, service_name, request_method, request_path, status)
            VALUES (#{req.permissionName}, #{req.permissionCode}, #{req.serviceName}, #{req.requestMethod}, #{req.requestPath}, #{status})
            """)
    int insert(@Param("req") ApiPermissionRequest request, @Param("status") Integer status);

    @Update("""
            UPDATE sys_api_permission
            SET permission_name = #{req.permissionName}, permission_code = #{req.permissionCode}, service_name = #{req.serviceName},
                request_method = #{req.requestMethod}, request_path = #{req.requestPath}, status = #{status}
            WHERE id = #{id}
            """)
    int update(@Param("id") Long id, @Param("req") ApiPermissionRequest request, @Param("status") Integer status);

    @Update("UPDATE sys_api_permission SET status = 0 WHERE id = #{id}")
    int disable(@Param("id") Long id);

    @Select("""
            SELECT id, permission_name AS permissionName, permission_code AS permissionCode,
                   service_name AS serviceName, request_method AS requestMethod, request_path AS requestPath, status
            FROM sys_api_permission
            WHERE status = 1
              AND (request_method = #{method} OR request_method = 'ALL')
            ORDER BY LENGTH(request_path) DESC, id ASC
            """)
    List<Map<String, Object>> listEnabledByMethod(@Param("method") String method);

    @Select("""
            SELECT COUNT(1)
            FROM sys_user u
            INNER JOIN sys_user_role ur ON ur.user_id = u.id
            INNER JOIN sys_role r ON r.id = ur.role_id
            INNER JOIN sys_role_api_permission rap ON rap.role_id = r.id
            INNER JOIN sys_api_permission ap ON ap.id = rap.api_permission_id
            WHERE u.id = #{userId}
              AND u.status = 1
              AND u.is_deleted = 0
              AND r.status = 1
              AND r.is_deleted = 0
              AND ap.status = 1
              AND ap.id = #{permissionId}
            """)
    int countUserPermission(@Param("userId") Long userId, @Param("permissionId") Long permissionId);
}

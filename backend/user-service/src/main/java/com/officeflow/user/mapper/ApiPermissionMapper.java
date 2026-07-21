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
}

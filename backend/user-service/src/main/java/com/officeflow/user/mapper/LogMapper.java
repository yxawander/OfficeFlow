package com.officeflow.user.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface LogMapper {
    @Insert("""
            INSERT INTO sys_login_log (user_id, username, login_ip, user_agent, login_status, message)
            VALUES (#{userId}, #{username}, #{ip}, #{userAgent}, #{status}, #{message})
            """)
    int insertLoginLog(@Param("userId") Long userId, @Param("username") String username,
                       @Param("ip") String ip, @Param("userAgent") String userAgent,
                       @Param("status") String status, @Param("message") String message);

    @Insert("""
            INSERT INTO sys_operation_log (user_id, username, module_name, operation_type, request_method, request_path, success, ip)
            VALUES (#{userId}, #{username}, #{moduleName}, #{operationType}, #{method}, #{path}, 1, #{ip})
            """)
    int insertOperationLog(@Param("userId") Long userId, @Param("username") String username,
                           @Param("moduleName") String moduleName, @Param("operationType") String operationType,
                           @Param("method") String method, @Param("path") String path, @Param("ip") String ip);

    @Select("""
            SELECT id, user_id AS userId, username, login_ip AS loginIp, login_status AS loginStatus, message, created_at AS createdAt
            FROM sys_login_log
            ORDER BY id DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> listLoginLogs(@Param("limit") int limit);

    @Select("""
            SELECT id, user_id AS userId, username, module_name AS moduleName, operation_type AS operationType,
                   request_method AS requestMethod, request_path AS requestPath, success, created_at AS createdAt
            FROM sys_operation_log
            ORDER BY id DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> listOperationLogs(@Param("limit") int limit);
}

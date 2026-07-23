package com.officeflow.user.mapper;

import com.officeflow.user.dto.UserRequest;
import com.officeflow.user.dto.ProfileUpdateRequest;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {

    @Select("""
            SELECT u.id, u.username, u.password, u.real_name AS realName, u.gender, u.phone, u.email, u.avatar,
                   u.dept_id AS deptId, d.dept_name AS deptName, u.post_id AS postId, p.post_name AS postName,
                   u.manager_id AS managerId, m.real_name AS managerName, u.hire_date AS hireDate,
                   u.user_type AS userType, u.status, u.last_login_at AS lastLoginAt
            FROM sys_user u
            LEFT JOIN sys_dept d ON d.id = u.dept_id
            LEFT JOIN sys_post p ON p.id = u.post_id
            LEFT JOIN sys_user m ON m.id = u.manager_id
            WHERE u.username = #{username} AND u.is_deleted = 0
            """)
    Map<String, Object> findByUsername(@Param("username") String username);

    @Select("""
            SELECT u.id, u.username, u.real_name AS realName, u.gender, u.phone, u.email, u.avatar,
                   u.dept_id AS deptId, d.dept_name AS deptName, u.post_id AS postId, p.post_name AS postName,
                   u.manager_id AS managerId, m.real_name AS managerName, u.hire_date AS hireDate,
                   u.user_type AS userType, u.status, u.last_login_at AS lastLoginAt
            FROM sys_user u
            LEFT JOIN sys_dept d ON d.id = u.dept_id
            LEFT JOIN sys_post p ON p.id = u.post_id
            LEFT JOIN sys_user m ON m.id = u.manager_id
            WHERE u.id = #{id} AND u.is_deleted = 0
            """)
    Map<String, Object> findProfileById(@Param("id") Long id);

    @Select("SELECT password FROM sys_user WHERE id = #{id} AND is_deleted = 0")
    String findPasswordById(@Param("id") Long id);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM sys_user u
            WHERE u.is_deleted = 0
              <if test="keyword != null and keyword != ''">
                AND (u.username LIKE CONCAT('%', #{keyword}, '%') OR u.real_name LIKE CONCAT('%', #{keyword}, '%') OR u.phone LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test="deptId != null">AND u.dept_id = #{deptId}</if>
              <if test="status != null">AND u.status = #{status}</if>
            </script>
            """)
    long countUsers(@Param("keyword") String keyword, @Param("deptId") Long deptId, @Param("status") Integer status);

    @Select("""
            <script>
            SELECT u.id, u.username, u.real_name AS realName, u.gender, u.phone, u.email,
                   u.dept_id AS deptId, d.dept_name AS deptName, u.post_id AS postId, p.post_name AS postName,
                   u.manager_id AS managerId, m.real_name AS managerName, u.hire_date AS hireDate,
                   u.user_type AS userType, u.status, u.created_at AS createdAt
            FROM sys_user u
            LEFT JOIN sys_dept d ON d.id = u.dept_id
            LEFT JOIN sys_post p ON p.id = u.post_id
            LEFT JOIN sys_user m ON m.id = u.manager_id
            WHERE u.is_deleted = 0
              <if test="keyword != null and keyword != ''">
                AND (u.username LIKE CONCAT('%', #{keyword}, '%') OR u.real_name LIKE CONCAT('%', #{keyword}, '%') OR u.phone LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test="deptId != null">AND u.dept_id = #{deptId}</if>
              <if test="status != null">AND u.status = #{status}</if>
            ORDER BY u.id ASC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<Map<String, Object>> listUsers(@Param("keyword") String keyword, @Param("deptId") Long deptId,
                                        @Param("status") Integer status, @Param("offset") long offset,
                                        @Param("pageSize") long pageSize);

    @Select("""
            SELECT id, username, real_name AS realName, dept_id AS deptId, post_id AS postId, manager_id AS managerId, status
            FROM sys_user
            WHERE is_deleted = 0 AND status = 1
            ORDER BY id ASC
            """)
    List<Map<String, Object>> listEnabledUsers();

    @Insert("""
            INSERT INTO sys_user (username, password, real_name, gender, phone, email, avatar, dept_id, post_id, manager_id, hire_date, user_type, status)
            VALUES (#{req.username}, #{password}, #{req.realName}, #{req.gender}, #{req.phone}, #{req.email}, #{req.avatar}, #{req.deptId},
                    #{req.postId}, #{req.managerId}, #{req.hireDate}, #{userType}, #{status})
            """)
    int insertUser(@Param("req") UserRequest request, @Param("password") String password,
                   @Param("userType") String userType, @Param("status") Integer status);

    @Update("""
            UPDATE sys_user
            SET real_name = #{req.realName}, gender = #{req.gender}, phone = #{req.phone}, email = #{req.email}, avatar = #{req.avatar},
                dept_id = #{req.deptId}, post_id = #{req.postId}, manager_id = #{req.managerId}, hire_date = #{req.hireDate},
                user_type = #{userType}, status = #{status}
            WHERE id = #{id} AND is_deleted = 0
            """)
    int updateUser(@Param("id") Long id, @Param("req") UserRequest request,
                   @Param("userType") String userType, @Param("status") Integer status);

    @Update("""
            UPDATE sys_user
            SET real_name = #{req.realName}, gender = #{req.gender}, phone = #{req.phone},
                email = #{req.email}, avatar = #{req.avatar}
            WHERE id = #{id} AND is_deleted = 0
            """)
    int updateProfile(@Param("id") Long id, @Param("req") ProfileUpdateRequest request);

    @Update("UPDATE sys_user SET is_deleted = 1 WHERE id = #{id} AND id <> 1")
    int softDelete(@Param("id") Long id);

    @Update("UPDATE sys_user SET status = #{status} WHERE id = #{id} AND is_deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE sys_user SET password = #{password} WHERE id = #{id} AND is_deleted = 0")
    int resetPassword(@Param("id") Long id, @Param("password") String password);

    @Update("UPDATE sys_user SET last_login_at = NOW() WHERE id = #{id}")
    int updateLastLoginAt(@Param("id") Long id);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteUserRoles(@Param("userId") Long userId);

    @Insert("""
            <script>
            INSERT INTO sys_user_role (user_id, role_id) VALUES
            <foreach collection="roleIds" item="roleId" separator=",">
              (#{userId}, #{roleId})
            </foreach>
            </script>
            """)
    int insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    @Update("UPDATE sys_user SET user_type = #{userType}, manager_id = #{managerId} WHERE id = #{id} AND is_deleted = 0")
    int updateUserTypeAndManager(@Param("id") Long id, @Param("userType") String userType, @Param("managerId") Long managerId);
}

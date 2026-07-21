package com.officeflow.user.mapper;

import com.officeflow.user.dto.DeptRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface DeptMapper {
    @Select("""
            SELECT d.id, d.parent_id AS parentId, d.dept_name AS deptName, d.dept_code AS deptCode,
                   d.leader_id AS leaderId, u.real_name AS leaderName, d.phone, d.email,
                   d.sort_order AS sortOrder, d.status
            FROM sys_dept d
            LEFT JOIN sys_user u ON u.id = d.leader_id
            WHERE d.is_deleted = 0
            ORDER BY d.parent_id ASC, d.sort_order ASC, d.id ASC
            """)
    List<Map<String, Object>> listAll();

    @Insert("""
            INSERT INTO sys_dept (parent_id, dept_name, dept_code, leader_id, phone, email, sort_order, status)
            VALUES (#{parentId}, #{req.deptName}, #{req.deptCode}, #{req.leaderId}, #{req.phone}, #{req.email}, #{sortOrder}, #{status})
            """)
    int insert(@Param("req") DeptRequest request, @Param("parentId") Long parentId,
               @Param("sortOrder") Integer sortOrder, @Param("status") Integer status);

    @Update("""
            UPDATE sys_dept
            SET parent_id = #{parentId}, dept_name = #{req.deptName}, dept_code = #{req.deptCode}, leader_id = #{req.leaderId},
                phone = #{req.phone}, email = #{req.email}, sort_order = #{sortOrder}, status = #{status}
            WHERE id = #{id} AND is_deleted = 0
            """)
    int update(@Param("id") Long id, @Param("req") DeptRequest request, @Param("parentId") Long parentId,
               @Param("sortOrder") Integer sortOrder, @Param("status") Integer status);

    @Update("UPDATE sys_dept SET is_deleted = 1 WHERE id = #{id}")
    int softDelete(@Param("id") Long id);
}

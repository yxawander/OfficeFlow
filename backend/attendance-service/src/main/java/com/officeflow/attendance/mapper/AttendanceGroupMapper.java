package com.officeflow.attendance.mapper;

import com.officeflow.attendance.dto.AttendanceGroupRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface AttendanceGroupMapper {

    @Select("""
            SELECT g.id, g.group_name AS groupName, g.rule_id AS ruleId, r.rule_name AS ruleName,
                   r.work_start_time AS workStartTime, r.work_end_time AS workEndTime,
                   g.dept_id AS deptId, d.dept_name AS deptName, g.status, g.created_at AS createdAt
            FROM attendance_group g
            LEFT JOIN attendance_rule r ON r.id = g.rule_id
            LEFT JOIN sys_dept d ON d.id = g.dept_id
            ORDER BY g.id ASC
            """)
    List<Map<String, Object>> selectAllGroups();

    @Select("""
            SELECT id, group_name AS groupName, rule_id AS ruleId, dept_id AS deptId, status
            FROM attendance_group
            WHERE id = #{id}
            """)
    Map<String, Object> selectGroupById(@Param("id") Long id);

    @Insert("""
            INSERT INTO attendance_group (group_name, rule_id, dept_id, status)
            VALUES (#{req.groupName}, #{req.ruleId}, #{req.deptId}, 1)
            """)
    int insertGroup(@Param("req") AttendanceGroupRequest request);

    @Update("""
            UPDATE attendance_group
            SET group_name = #{req.groupName},
                rule_id = #{req.ruleId},
                dept_id = #{req.deptId}
            WHERE id = #{id}
            """)
    int updateGroup(@Param("id") Long id, @Param("req") AttendanceGroupRequest request);
}

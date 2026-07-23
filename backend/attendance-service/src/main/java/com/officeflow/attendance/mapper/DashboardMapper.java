package com.officeflow.attendance.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 数据大屏聚合查询 Mapper
 */
@Mapper
public interface DashboardMapper {

    /**
     * 大屏总览：员工总数、今日打卡、迟到/早退/缺勤、出勤率、待审批数、公告阅读率
     */
    @Select("""
            SELECT
                (SELECT COUNT(*) FROM sys_user WHERE is_deleted = 0 AND status = 1) AS totalUsers,
                (SELECT COUNT(*) FROM attendance_record WHERE work_date = CURDATE()) AS todayCheckIn,
                (SELECT COUNT(*) FROM attendance_record WHERE work_date = CURDATE() AND status IN ('LATE', 'LATE_AND_EARLY')) AS todayLate,
                (SELECT COUNT(*) FROM attendance_record WHERE work_date = CURDATE() AND status IN ('EARLY_LEAVE', 'LATE_AND_EARLY')) AS todayEarly,
                (SELECT COUNT(*) FROM sys_user WHERE is_deleted = 0 AND status = 1
                    AND id NOT IN (SELECT DISTINCT user_id FROM attendance_record WHERE work_date = CURDATE())) AS todayAbsent,
                (SELECT COUNT(*) FROM flow_apply WHERE status = 'PENDING' AND is_deleted = 0 AND approver_id = #{userId}) AS pendingApprovals,
                (SELECT COALESCE(ROUND(AVG(read_count * 100.0 /
                    (SELECT COUNT(*) FROM sys_user WHERE is_deleted = 0 AND status = 1)), 1), 0)
                    FROM notice WHERE status = 'PUBLISHED' AND is_deleted = 0) AS noticeReadRate
            """)
    Map<String, Object> selectOverview(@Param("userId") Long userId);

    /**
     * 本周考勤趋势：周一~周五每天按状态分组统计
     */
    @Select("""
            SELECT
                DATE_FORMAT(work_date, '%m/%d') AS day,
                WEEKDAY(work_date) AS dayIndex,
                SUM(CASE WHEN status NOT IN ('LATE', 'EARLY_LEAVE', 'LATE_AND_EARLY', 'MISSING_CARD', 'ABSENT') THEN 1 ELSE 0 END) AS normal,
                SUM(CASE WHEN status IN ('LATE', 'LATE_AND_EARLY') THEN 1 ELSE 0 END) AS late,
                SUM(CASE WHEN status IN ('EARLY_LEAVE', 'LATE_AND_EARLY') THEN 1 ELSE 0 END) AS earlyLeave,
                SUM(CASE WHEN status = 'MISSING_CARD' THEN 1 ELSE 0 END) AS missingCard
            FROM attendance_record
            WHERE work_date >= DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE())) DAY)
              AND work_date <= CURDATE()
            GROUP BY work_date
            ORDER BY work_date
            """)
    List<Map<String, Object>> selectWeeklyTrend();

    /**
     * 部门周出勤热力：每个部门近 7 天每天的出勤人数和应到人数
     */
    @Select("""
            SELECT
                d.id AS deptId,
                d.dept_name AS deptName,
                (SELECT COUNT(*) FROM sys_user u WHERE u.dept_id = d.id AND u.is_deleted = 0 AND u.status = 1) AS total,
                ar.work_date AS workDate,
                DAYOFWEEK(ar.work_date) - 1 AS dayIndex,
                COUNT(ar.id) AS present
            FROM sys_dept d
            LEFT JOIN attendance_record ar ON ar.dept_id = d.id
                AND ar.work_date >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
                AND ar.work_date <= CURDATE()
            WHERE d.is_deleted = 0 AND d.status = 1
            GROUP BY d.id, d.dept_name, ar.work_date
            ORDER BY d.id, ar.work_date
            """)
    List<Map<String, Object>> selectDeptHeatmap();

    /**
     * 审批类型分布：本月各类型审批数量
     */
    @Select("""
            SELECT apply_type AS applyType, COUNT(*) AS count
            FROM flow_apply
            WHERE YEAR(created_at) = YEAR(CURDATE())
              AND MONTH(created_at) = MONTH(CURDATE())
              AND is_deleted = 0
            GROUP BY apply_type
            """)
    List<Map<String, Object>> selectFlowDistribution();

    /**
     * 查询用户角色数据范围和所属部门
     */
    @Select("""
            SELECT r.data_scope AS dataScope, u.dept_id AS deptId
            FROM sys_user u
            INNER JOIN sys_user_role ur ON ur.user_id = u.id
            INNER JOIN sys_role r ON r.id = ur.role_id
            WHERE u.id = #{userId} AND r.is_deleted = 0
            LIMIT 1
            """)
    Map<String, Object> selectUserScope(@Param("userId") Long userId);

    /**
     * 部门范围总览：MANAGER 看本部门及子部门数据
     */
    @Select("""
            SELECT
                (SELECT COUNT(*) FROM sys_user WHERE is_deleted = 0 AND status = 1
                    AND dept_id IN (SELECT id FROM sys_dept WHERE id = #{deptId} OR parent_id = #{deptId})) AS totalUsers,
                (SELECT COUNT(*) FROM attendance_record WHERE work_date = CURDATE()
                    AND dept_id IN (SELECT id FROM sys_dept WHERE id = #{deptId} OR parent_id = #{deptId})) AS todayCheckIn,
                (SELECT COUNT(*) FROM attendance_record WHERE work_date = CURDATE()
                    AND dept_id IN (SELECT id FROM sys_dept WHERE id = #{deptId} OR parent_id = #{deptId})
                    AND status IN ('LATE', 'LATE_AND_EARLY')) AS todayLate,
                (SELECT COUNT(*) FROM attendance_record WHERE work_date = CURDATE()
                    AND dept_id IN (SELECT id FROM sys_dept WHERE id = #{deptId} OR parent_id = #{deptId})
                    AND status IN ('EARLY_LEAVE', 'LATE_AND_EARLY')) AS todayEarly,
                (SELECT COUNT(*) FROM sys_user WHERE is_deleted = 0 AND status = 1
                    AND dept_id IN (SELECT id FROM sys_dept WHERE id = #{deptId} OR parent_id = #{deptId})
                    AND id NOT IN (SELECT DISTINCT user_id FROM attendance_record WHERE work_date = CURDATE())) AS todayAbsent,
                (SELECT COUNT(*) FROM flow_apply WHERE status = 'PENDING' AND is_deleted = 0
                    AND approver_id = #{userId}) AS pendingApprovals,
                (SELECT COALESCE(ROUND(AVG(read_count * 100.0 /
                    GREATEST((SELECT COUNT(*) FROM sys_user WHERE is_deleted = 0 AND status = 1
                        AND dept_id IN (SELECT id FROM sys_dept WHERE id = #{deptId} OR parent_id = #{deptId})), 1)), 1), 0)
                    FROM notice WHERE status = 'PUBLISHED' AND is_deleted = 0) AS noticeReadRate
            """)
    Map<String, Object> selectOverviewByDept(@Param("userId") Long userId, @Param("deptId") Long deptId);

    /**
     * 部门范围热力图：仅本部门及子部门
     */
    @Select("""
            SELECT
                d.id AS deptId,
                d.dept_name AS deptName,
                (SELECT COUNT(*) FROM sys_user u WHERE u.dept_id = d.id AND u.is_deleted = 0 AND u.status = 1) AS total,
                ar.work_date AS workDate,
                DAYOFWEEK(ar.work_date) - 1 AS dayIndex,
                COUNT(ar.id) AS present
            FROM sys_dept d
            LEFT JOIN attendance_record ar ON ar.dept_id = d.id
                AND ar.work_date >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
                AND ar.work_date <= CURDATE()
            WHERE d.is_deleted = 0 AND d.status = 1
                AND (d.id = #{deptId} OR d.parent_id = #{deptId})
            GROUP BY d.id, d.dept_name, ar.work_date
            ORDER BY d.id, ar.work_date
            """)
    List<Map<String, Object>> selectDeptHeatmapByDept(@Param("deptId") Long deptId);

    /**
     * 部门范围审批分布：本部门及子部门员工的审批
     */
    @Select("""
            SELECT fa.apply_type AS applyType, COUNT(*) AS count
            FROM flow_apply fa
            WHERE YEAR(fa.created_at) = YEAR(CURDATE())
              AND MONTH(fa.created_at) = MONTH(CURDATE())
              AND fa.is_deleted = 0
              AND fa.applicant_dept_id IN
                  (SELECT id FROM sys_dept WHERE id = #{deptId} OR parent_id = #{deptId})
            GROUP BY fa.apply_type
            """)
    List<Map<String, Object>> selectFlowDistributionByDept(@Param("deptId") Long deptId);
}

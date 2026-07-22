package com.officeflow.attendance.mapper;

import com.officeflow.attendance.entity.SalaryMonthlyStatement;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface SalaryMonthlyStatementMapper {

    @Insert("""
            INSERT INTO salary_monthly_statement (user_id, settle_month, base_salary, overtime_pay, allowance, late_deduction, absent_deduction, leave_deduction, actual_salary, status, created_at)
            VALUES (#{userId}, #{settleMonth}, #{baseSalary}, #{overtimePay}, #{allowance}, #{lateDeduction}, #{absentDeduction}, #{leaveDeduction}, #{actualSalary}, #{status}, NOW())
            ON DUPLICATE KEY UPDATE
                base_salary = VALUES(base_salary),
                overtime_pay = VALUES(overtime_pay),
                allowance = VALUES(allowance),
                late_deduction = VALUES(late_deduction),
                absent_deduction = VALUES(absent_deduction),
                leave_deduction = VALUES(leave_deduction),
                actual_salary = VALUES(actual_salary),
                status = VALUES(status)
            """)
    int upsertStatement(SalaryMonthlyStatement statement);

    @Select("""
            <script>
            SELECT s.id, s.user_id AS userId, s.settle_month AS settleMonth,
                   s.base_salary AS baseSalary, s.overtime_pay AS overtimePay,
                   s.allowance, s.late_deduction AS lateDeduction,
                   s.absent_deduction AS absentDeduction, s.leave_deduction AS leaveDeduction,
                   s.actual_salary AS actualSalary, s.status, s.created_at AS createdAt,
                   u.real_name AS realName, u.username, d.dept_name AS deptName, p.post_name AS postName
            FROM salary_monthly_statement s
            LEFT JOIN sys_user u ON u.id = s.user_id
            LEFT JOIN sys_dept d ON d.id = u.dept_id
            LEFT JOIN sys_post p ON p.id = u.post_id
            WHERE s.settle_month = #{settleMonth}
              <if test="deptId != null">AND u.dept_id = #{deptId}</if>
              <if test="keyword != null and keyword != ''">
                AND (u.real_name LIKE CONCAT('%', #{keyword}, '%') OR u.username LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            ORDER BY u.id ASC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<SalaryMonthlyStatement> selectStatements(@Param("settleMonth") String settleMonth,
                                                  @Param("deptId") Long deptId,
                                                  @Param("keyword") String keyword,
                                                  @Param("offset") long offset,
                                                  @Param("pageSize") long pageSize);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM salary_monthly_statement s
            LEFT JOIN sys_user u ON u.id = s.user_id
            WHERE s.settle_month = #{settleMonth}
              <if test="deptId != null">AND u.dept_id = #{deptId}</if>
              <if test="keyword != null and keyword != ''">
                AND (u.real_name LIKE CONCAT('%', #{keyword}, '%') OR u.username LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            </script>
            """)
    long countStatements(@Param("settleMonth") String settleMonth,
                        @Param("deptId") Long deptId,
                        @Param("keyword") String keyword);

    @Select("""
            SELECT s.id, s.user_id AS userId, s.settle_month AS settleMonth,
                   s.base_salary AS baseSalary, s.overtime_pay AS overtimePay,
                   s.allowance, s.late_deduction AS lateDeduction,
                   s.absent_deduction AS absentDeduction, s.leave_deduction AS leaveDeduction,
                   s.actual_salary AS actualSalary, s.status, s.created_at AS createdAt,
                   u.real_name AS realName, u.username, d.dept_name AS deptName, p.post_name AS postName
            FROM salary_monthly_statement s
            LEFT JOIN sys_user u ON u.id = s.user_id
            LEFT JOIN sys_dept d ON d.id = u.dept_id
            LEFT JOIN sys_post p ON p.id = u.post_id
            WHERE s.user_id = #{userId} AND s.settle_month = #{settleMonth}
            LIMIT 1
            """)
    SalaryMonthlyStatement selectStatementByUserIdAndMonth(@Param("userId") Long userId, @Param("settleMonth") String settleMonth);

    @Select("""
            SELECT
              COALESCE(us.base_salary, p.base_salary, 8000.00) AS baseSalary,
              COALESCE(us.allowance, 500.00) AS allowance
            FROM sys_user u
            LEFT JOIN sys_user_salary us ON us.user_id = u.id
            LEFT JOIN sys_post p ON p.id = u.post_id
            WHERE u.id = #{userId}
            """)
    Map<String, Object> getUserSalaryConfig(@Param("userId") Long userId);

    @Select("""
            SELECT COALESCE(SUM(late_minutes + early_leave_minutes), 0)
            FROM attendance_record
            WHERE user_id = #{userId} AND DATE_FORMAT(work_date, '%Y-%m') = #{settleMonth}
            """)
    Integer selectSumLateAndEarlyMinutes(@Param("userId") Long userId, @Param("settleMonth") String settleMonth);
}

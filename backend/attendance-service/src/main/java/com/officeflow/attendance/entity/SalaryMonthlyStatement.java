package com.officeflow.attendance.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SalaryMonthlyStatement {
    private Long id;
    private Long userId;
    private String settleMonth; // YYYY-MM
    private BigDecimal baseSalary;
    private BigDecimal overtimePay;
    private BigDecimal allowance;
    private BigDecimal lateDeduction;
    private BigDecimal absentDeduction;
    private BigDecimal leaveDeduction;
    private BigDecimal actualSalary;
    private String status; // DRAFT, PUBLISHED
    private LocalDateTime createdAt;

    // Snapshot fields for detailed calculation breakdown
    private BigDecimal dailyWage;
    private BigDecimal hourlyWage;
    private BigDecimal overtimeHours;
    private BigDecimal offWorkHours;
    private BigDecimal absentDays;
    private BigDecimal leaveDays;

    // Extra fields for VO display
    private String username;
    private String realName;
    private String deptName;
    private String postName;
}

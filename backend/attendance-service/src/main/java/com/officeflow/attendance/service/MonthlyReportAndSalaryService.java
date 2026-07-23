package com.officeflow.attendance.service;

import com.officeflow.attendance.entity.AttendanceMonthlyReport;
import com.officeflow.attendance.entity.SalaryMonthlyStatement;
import com.officeflow.attendance.mapper.AttendanceMonthlyReportMapper;
import com.officeflow.attendance.mapper.SalaryMonthlyStatementMapper;
import com.officeflow.common.api.PageResult;
import com.officeflow.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyReportAndSalaryService {

    private final AttendanceMonthlyReportMapper monthlyReportMapper;
    private final SalaryMonthlyStatementMapper salaryStatementMapper;

    /**
     * 生成/更新全员指定月份的月度考勤报表
     */
    @Transactional
    public void generateMonthlyReport(String reportMonth) {
        if (reportMonth == null || !reportMonth.matches("^\\d{4}-\\d{2}$")) {
            throw new BusinessException("月份格式错误，示例：2026-07");
        }

        List<Map<String, Object>> users = monthlyReportMapper.listAllActiveUsers();
        for (Map<String, Object> user : users) {
            Long userId = Long.parseLong(user.get("userId").toString());
            Long deptId = user.get("deptId") != null ? Long.parseLong(user.get("deptId").toString()) : null;

            Map<String, Object> stats = monthlyReportMapper.calculateUserAttendanceStats(userId, reportMonth);
            int actualWorkDays = stats != null && stats.get("actualWorkDays") != null ? Integer.parseInt(stats.get("actualWorkDays").toString()) : 0;
            int lateCount = stats != null && stats.get("lateCount") != null ? Integer.parseInt(stats.get("lateCount").toString()) : 0;
            int earlyLeaveCount = stats != null && stats.get("earlyLeaveCount") != null ? Integer.parseInt(stats.get("earlyLeaveCount").toString()) : 0;
            int absentCount = stats != null && stats.get("absentCount") != null ? Integer.parseInt(stats.get("absentCount").toString()) : 0;
            int missingCardCount = stats != null && stats.get("missingCardCount") != null ? Integer.parseInt(stats.get("missingCardCount").toString()) : 0;

            BigDecimal leaveDays = monthlyReportMapper.calculateApprovedLeaveDays(userId, reportMonth);
            if (leaveDays == null) {
                leaveDays = BigDecimal.ZERO;
            }

            // 计算加班时长：采用 Min 规则 (Min(审批加班时长, 实际打卡加班时长))
            List<Map<String, Object>> overtimeApplies = monthlyReportMapper.selectApprovedOvertimeApplies(userId, reportMonth);
            double totalOvertimeHours = 0.0;
            for (Map<String, Object> apply : overtimeApplies) {
                double approvedHours = apply.get("approvedHours") != null ? Double.parseDouble(apply.get("approvedHours").toString()) : 0.0;
                Object startTime = apply.get("startTime");
                Object endTime = apply.get("endTime");
                Double actualHours = monthlyReportMapper.selectActualOvertimeHours(userId, startTime, endTime);
                if (actualHours == null) {
                    actualHours = approvedHours; // 如果那天无打卡流（如周末加班），以审批时间为准
                }
                double validHours = Math.min(approvedHours, Math.max(0.0, actualHours));
                totalOvertimeHours += validHours;
            }

            AttendanceMonthlyReport report = new AttendanceMonthlyReport();
            report.setUserId(userId);
            report.setDeptId(deptId);
            report.setReportMonth(reportMonth);
            report.setShouldWorkDays(22); // 假设法定月出勤 22 天
            report.setActualWorkDays(actualWorkDays);
            report.setLateCount(lateCount);
            report.setEarlyLeaveCount(earlyLeaveCount);
            report.setAbsentCount(absentCount);
            report.setMissingCardCount(missingCardCount);
            report.setLeaveDays(leaveDays.setScale(1, RoundingMode.HALF_UP));
            report.setOvertimeHours(BigDecimal.valueOf(totalOvertimeHours).setScale(1, RoundingMode.HALF_UP));

            monthlyReportMapper.upsertReport(report);
        }
    }

    /**
     * 分页查询月度考勤报表
     */
    public PageResult<AttendanceMonthlyReport> getMonthlyReports(String reportMonth, Long deptId, String keyword, long pageNum, long pageSize) {
        long safePage = Math.max(pageNum, 1);
        long safeSize = Math.min(Math.max(pageSize, 1), 100);
        long offset = (safePage - 1) * safeSize;

        List<AttendanceMonthlyReport> records = monthlyReportMapper.selectReports(reportMonth, deptId, keyword, offset, safeSize);
        long total = monthlyReportMapper.countReports(reportMonth, deptId, keyword);

        return PageResult.of(total, safePage, safeSize, records);
    }

    /**
     * 生成/计算全员指定月份的月度工资条
     */
    @Transactional
    public void generateMonthlySalary(String settleMonth) {
        if (settleMonth == null || !settleMonth.matches("^\\d{4}-\\d{2}$")) {
            throw new BusinessException("月份格式错误，示例：2026-07");
        }

        // 先确保月度考勤报表是最新的
        generateMonthlyReport(settleMonth);

        List<Map<String, Object>> users = monthlyReportMapper.listAllActiveUsers();
        for (Map<String, Object> user : users) {
            Long userId = Long.parseLong(user.get("userId").toString());

            Map<String, Object> salaryConfig = salaryStatementMapper.getUserSalaryConfig(userId);
            BigDecimal baseSalary = salaryConfig.get("baseSalary") != null ? new BigDecimal(salaryConfig.get("baseSalary").toString()) : new BigDecimal("8000.00");
            BigDecimal allowance = salaryConfig.get("allowance") != null ? new BigDecimal(salaryConfig.get("allowance").toString()) : new BigDecimal("500.00");

            // 日薪 = baseSalary / 21.75
            BigDecimal dailyRate = baseSalary.divide(new BigDecimal("21.75"), 4, RoundingMode.HALF_UP);
            BigDecimal hourlyRate = dailyRate.divide(new BigDecimal("8.0"), 4, RoundingMode.HALF_UP);

            // 获取月度考勤数据
            List<AttendanceMonthlyReport> userReports = monthlyReportMapper.selectReports(settleMonth, null, null, 0, 1000);
            AttendanceMonthlyReport report = userReports.stream().filter(r -> r.getUserId().equals(userId)).findFirst().orElse(null);

            int lateCount = report != null ? report.getLateCount() : 0;
            int absentCount = report != null ? report.getAbsentCount() : 0;
            double leaveDaysVal = report != null && report.getLeaveDays() != null ? report.getLeaveDays().doubleValue() : 0.0;
            double overtimeHoursVal = report != null && report.getOvertimeHours() != null ? report.getOvertimeHours().doubleValue() : 0.0;

            // 加班费 = overtimeHours * hourlyRate * 1.5
            BigDecimal overtimePay = hourlyRate.multiply(BigDecimal.valueOf(overtimeHoursVal)).multiply(new BigDecimal("1.5")).setScale(2, RoundingMode.HALF_UP);

            // 迟到早退及未补卡缺卡扣款 = 实际离岗小时数 (迟到+早退分钟数 / 60 + 未补卡缺卡次数 * 4小时) * 时薪 (hourlyRate)
            Integer offWorkMinutes = salaryStatementMapper.selectSumLateAndEarlyMinutes(userId, settleMonth);
            int missingCardCount = report != null ? report.getMissingCardCount() : 0;
            double offWorkHours = ((offWorkMinutes != null ? offWorkMinutes : 0) / 60.0) + (missingCardCount * 4.0);
            BigDecimal lateDeduction = hourlyRate.multiply(BigDecimal.valueOf(offWorkHours)).setScale(2, RoundingMode.HALF_UP);

            // 旷工扣款 = 旷工天数 * dailyRate * 2.0
            BigDecimal absentDeduction = dailyRate.multiply(BigDecimal.valueOf(absentCount)).multiply(new BigDecimal("2.0")).setScale(2, RoundingMode.HALF_UP);

            // 请假扣款 = 请假天数 * dailyRate
            BigDecimal leaveDeduction = dailyRate.multiply(BigDecimal.valueOf(leaveDaysVal)).setScale(2, RoundingMode.HALF_UP);

            // 应发总实发 = baseSalary + allowance + overtimePay - lateDeduction - absentDeduction - leaveDeduction
            BigDecimal actualSalary = baseSalary.add(allowance).add(overtimePay)
                    .subtract(lateDeduction).subtract(absentDeduction).subtract(leaveDeduction);
            if (actualSalary.compareTo(BigDecimal.ZERO) < 0) {
                actualSalary = BigDecimal.ZERO;
            }
            actualSalary = actualSalary.setScale(2, RoundingMode.HALF_UP);

            SalaryMonthlyStatement statement = new SalaryMonthlyStatement();
            statement.setUserId(userId);
            statement.setSettleMonth(settleMonth);
            statement.setBaseSalary(baseSalary);
            statement.setOvertimePay(overtimePay);
            statement.setAllowance(allowance);
            statement.setLateDeduction(lateDeduction);
            statement.setAbsentDeduction(absentDeduction);
            statement.setLeaveDeduction(leaveDeduction);
            statement.setActualSalary(actualSalary);
            statement.setStatus("PUBLISHED");

            salaryStatementMapper.upsertStatement(statement);
        }
    }

    /**
     * 分页查询全员工资条
     */
    public PageResult<SalaryMonthlyStatement> getSalaryStatements(String settleMonth, Long deptId, String keyword, long pageNum, long pageSize) {
        long safePage = Math.max(pageNum, 1);
        long safeSize = Math.min(Math.max(pageSize, 1), 100);
        long offset = (safePage - 1) * safeSize;

        List<SalaryMonthlyStatement> records = salaryStatementMapper.selectStatements(settleMonth, deptId, keyword, offset, safeSize);
        long total = salaryStatementMapper.countStatements(settleMonth, deptId, keyword);

        return PageResult.of(total, safePage, safeSize, records);
    }

    /**
     * 查询个人特定月份工资条
     */
    public SalaryMonthlyStatement getMySalaryStatement(Long userId, String settleMonth) {
        return salaryStatementMapper.selectStatementByUserIdAndMonth(userId, settleMonth);
    }
}

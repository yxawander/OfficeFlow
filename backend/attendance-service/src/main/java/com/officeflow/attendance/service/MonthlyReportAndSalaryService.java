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
     * 计算指定月份的工作日（周一至周五）天数
     */
    private int calculateWorkDays(String yearMonthStr) {
        java.time.YearMonth yearMonth = java.time.YearMonth.parse(yearMonthStr);
        int daysInMonth = yearMonth.lengthOfMonth();
        int workDays = 0;
        for (int i = 1; i <= daysInMonth; i++) {
            java.time.DayOfWeek dayOfWeek = yearMonth.atDay(i).getDayOfWeek();
            if (dayOfWeek != java.time.DayOfWeek.SATURDAY && dayOfWeek != java.time.DayOfWeek.SUNDAY) {
                workDays++;
            }
        }
        return workDays;
    }

    /**
     * 生成/更新全员指定月份的月度考勤报表
     */
    @Transactional
    public void generateMonthlyReport(String reportMonth) {
        if (reportMonth == null || !reportMonth.matches("^\\d{4}-\\d{2}$")) {
            throw new BusinessException("月份格式错误，示例：2026-07");
        }
        
        int dynamicShouldWorkDays = calculateWorkDays(reportMonth);

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

            BigDecimal leaveDays = stats != null && stats.get("leaveDays") != null ? new BigDecimal(stats.get("leaveDays").toString()) : BigDecimal.ZERO;
            
            double totalOvertimeHours = stats != null && stats.get("overtimeHours") != null ? Double.parseDouble(stats.get("overtimeHours").toString()) : 0.0;

            AttendanceMonthlyReport report = new AttendanceMonthlyReport();
            report.setUserId(userId);
            report.setDeptId(deptId);
            report.setReportMonth(reportMonth);
            report.setShouldWorkDays(dynamicShouldWorkDays); // 动态计算当月法定工作日
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
    public PageResult<AttendanceMonthlyReport> getMonthlyReports(String reportMonth, Long deptId, String keyword, Long filterUserId, long pageNum, long pageSize) {
        long safePage = Math.max(pageNum, 1);
        long safeSize = Math.min(Math.max(pageSize, 1), 100);
        long offset = (safePage - 1) * safeSize;

        List<AttendanceMonthlyReport> records = monthlyReportMapper.selectReports(reportMonth, deptId, keyword, filterUserId, offset, safeSize);
        long total = monthlyReportMapper.countReports(reportMonth, deptId, keyword, filterUserId);

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
            List<AttendanceMonthlyReport> userReports = monthlyReportMapper.selectReports(settleMonth, null, null, null, 0, 1000);
            AttendanceMonthlyReport report = userReports.stream().filter(r -> r.getUserId().equals(userId)).findFirst().orElse(null);

            int absentCount = report != null ? report.getAbsentCount() : 0;
            double leaveDaysVal = report != null && report.getLeaveDays() != null ? report.getLeaveDays().doubleValue() : 0.0;
            double overtimeHoursVal = report != null && report.getOvertimeHours() != null ? report.getOvertimeHours().doubleValue() : 0.0;

            // 加班费 = overtimeHours * hourlyRate * 1.5
            BigDecimal overtimePay = hourlyRate.multiply(BigDecimal.valueOf(overtimeHoursVal)).multiply(new BigDecimal("1.5")).setScale(2, RoundingMode.HALF_UP);

            // ================== V2.0 薪资核算核心逻辑 (互斥与双轨制) ==================

            // 基础薪资池：扣除事假与旷工
            BigDecimal leaveDeduction = dailyRate.multiply(BigDecimal.valueOf(leaveDaysVal)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal absentDeduction = dailyRate.multiply(BigDecimal.valueOf(absentCount)).multiply(new BigDecimal("2.0")).setScale(2, RoundingMode.HALF_UP);
            
            BigDecimal actualBaseSalary = baseSalary.subtract(leaveDeduction).subtract(absentDeduction);
            if (actualBaseSalary.compareTo(BigDecimal.ZERO) < 0) {
                actualBaseSalary = BigDecimal.ZERO;
            }

            // 考勤绩效池：扣除迟到早退及缺卡违纪罚款 (绝不触碰底薪)
            BigDecimal performanceBonus = salaryConfig.get("performanceBonus") != null ? new BigDecimal(salaryConfig.get("performanceBonus").toString()) : new BigDecimal("500.00");
            
            Integer offWorkMinutes = salaryStatementMapper.selectSumLateAndEarlyMinutes(userId, settleMonth);
            int missingCardCount = report != null ? report.getMissingCardCount() : 0;
            
            // 互斥逻辑：如果全天旷工（is_absent == true），强制清零该日期的缺卡罚金。
            // 由于 monthlyReportMapper 统计 missingCardCount 时只看 is_missing_card，
            // 业务约束：Scheduler 或审批回写时保证 is_absent=1 时 is_missing_card 必然为0，所以此处缺卡次数本身已经互斥。
            
            double offWorkHours = (offWorkMinutes != null ? offWorkMinutes : 0) / 60.0;
            BigDecimal lateDeduction = hourlyRate.multiply(BigDecimal.valueOf(offWorkHours)).setScale(2, RoundingMode.HALF_UP);
            // 缺卡罚金 = 缺卡次数 * 4小时时薪
            BigDecimal missingCardDeduction = hourlyRate.multiply(BigDecimal.valueOf(missingCardCount * 4.0)).setScale(2, RoundingMode.HALF_UP);
            
            BigDecimal actualBonus = performanceBonus.subtract(lateDeduction).subtract(missingCardDeduction);
            if (actualBonus.compareTo(BigDecimal.ZERO) < 0) {
                actualBonus = BigDecimal.ZERO;
            }

            // 应发总实发 = 基础薪资余量 + 绩效余量 + 津贴 + 加班费
            BigDecimal actualSalary = actualBaseSalary.add(actualBonus).add(allowance).add(overtimePay).setScale(2, RoundingMode.HALF_UP);
            // =======================================================================

            SalaryMonthlyStatement statement = new SalaryMonthlyStatement();
            statement.setUserId(userId);
            statement.setSettleMonth(settleMonth);
            statement.setBaseSalary(baseSalary);
            statement.setOvertimePay(overtimePay);
            statement.setAllowance(allowance);
            statement.setPerformanceBonus(performanceBonus);
            statement.setLateDeduction(lateDeduction);
            statement.setMissingCardDeduction(missingCardDeduction);
            statement.setAbsentDeduction(absentDeduction);
            statement.setLeaveDeduction(leaveDeduction);
            statement.setActualSalary(actualSalary);
            statement.setStatus("DRAFT");
            
            // Set Snapshot fields
            statement.setDailyWage(dailyRate);
            statement.setHourlyWage(hourlyRate);
            statement.setOvertimeHours(BigDecimal.valueOf(overtimeHoursVal).setScale(1, RoundingMode.HALF_UP));
            statement.setOffWorkHours(BigDecimal.valueOf(offWorkHours).setScale(1, RoundingMode.HALF_UP));
            statement.setAbsentDays(BigDecimal.valueOf(absentCount).setScale(1, RoundingMode.HALF_UP));
            statement.setLeaveDays(BigDecimal.valueOf(leaveDaysVal).setScale(1, RoundingMode.HALF_UP));

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
     * 批量发布工资条
     */
    @Transactional
    public void publishSalaryStatements(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        salaryStatementMapper.updateStatusByIds(ids, "PUBLISHED");
    }

    /**
     * 查询个人特定月份工资条
     */
    public SalaryMonthlyStatement getMySalaryStatement(Long userId, String settleMonth) {
        return salaryStatementMapper.selectStatementByUserIdAndMonth(userId, settleMonth);
    }

    /**
     * 为单个用户重新计算指定月份的考勤报表和工资单
     */
    @Transactional
    public void recalculateUserSalaryAndReport(Long userId, String settleMonth) {
        int dynamicShouldWorkDays = calculateWorkDays(settleMonth);

        Map<String, Object> stats = monthlyReportMapper.calculateUserAttendanceStats(userId, settleMonth);
        int actualWorkDays = stats != null && stats.get("actualWorkDays") != null ? Integer.parseInt(stats.get("actualWorkDays").toString()) : 0;
        int lateCount = stats != null && stats.get("lateCount") != null ? Integer.parseInt(stats.get("lateCount").toString()) : 0;
        int earlyLeaveCount = stats != null && stats.get("earlyLeaveCount") != null ? Integer.parseInt(stats.get("earlyLeaveCount").toString()) : 0;
        int absentCount = stats != null && stats.get("absentCount") != null ? Integer.parseInt(stats.get("absentCount").toString()) : 0;
        int missingCardCount = stats != null && stats.get("missingCardCount") != null ? Integer.parseInt(stats.get("missingCardCount").toString()) : 0;

        BigDecimal leaveDays = stats != null && stats.get("leaveDays") != null ? new BigDecimal(stats.get("leaveDays").toString()) : BigDecimal.ZERO;
        
        double totalOvertimeHours = stats != null && stats.get("overtimeHours") != null ? Double.parseDouble(stats.get("overtimeHours").toString()) : 0.0;

        AttendanceMonthlyReport report = new AttendanceMonthlyReport();
        report.setUserId(userId);
        report.setReportMonth(settleMonth);
        report.setShouldWorkDays(dynamicShouldWorkDays);
        report.setActualWorkDays(actualWorkDays);
        report.setLateCount(lateCount);
        report.setEarlyLeaveCount(earlyLeaveCount);
        report.setMissingCardCount(missingCardCount);
        report.setAbsentCount(absentCount);
        report.setOvertimeHours(BigDecimal.valueOf(totalOvertimeHours).setScale(1, RoundingMode.HALF_UP));
        report.setLeaveDays(leaveDays.setScale(1, RoundingMode.HALF_UP));
        monthlyReportMapper.upsertReport(report);

        // 2. 重算该用户该月工资
        Map<String, Object> salaryConfig = salaryStatementMapper.getUserSalaryConfig(userId);
        BigDecimal baseSalary = salaryConfig.get("baseSalary") != null ? new BigDecimal(salaryConfig.get("baseSalary").toString()) : new BigDecimal("8000.00");
        BigDecimal allowance = salaryConfig.get("allowance") != null ? new BigDecimal(salaryConfig.get("allowance").toString()) : new BigDecimal("500.00");

        BigDecimal dailyRate = baseSalary.divide(new BigDecimal("21.75"), 4, RoundingMode.HALF_UP);
        BigDecimal hourlyRate = dailyRate.divide(new BigDecimal("8.0"), 4, RoundingMode.HALF_UP);

        BigDecimal overtimePay = report.getOvertimeHours().multiply(hourlyRate).multiply(new BigDecimal("1.5")).setScale(2, RoundingMode.HALF_UP);

        Integer offWorkMinutes = salaryStatementMapper.selectSumLateAndEarlyMinutes(userId, settleMonth);
        // missingCardCount is already defined above
        
        double offWorkHours = (offWorkMinutes != null ? offWorkMinutes : 0) / 60.0;
        BigDecimal lateDeduction = hourlyRate.multiply(BigDecimal.valueOf(offWorkHours)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal missingCardDeduction = hourlyRate.multiply(BigDecimal.valueOf(missingCardCount * 4.0)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal performanceBonus = salaryConfig.get("performanceBonus") != null ? new BigDecimal(salaryConfig.get("performanceBonus").toString()) : new BigDecimal("500.00");

        // V2.0 合规级：绩效池计算，迟到与缺卡只能从绩效中扣除，扣完即止，不触碰底薪
        BigDecimal actualBonus = performanceBonus.subtract(lateDeduction).subtract(missingCardDeduction);
        if (actualBonus.compareTo(BigDecimal.ZERO) < 0) {
            actualBonus = BigDecimal.ZERO;
        }

        // V2.0 合规级：底薪池计算，旷工与事假从底薪中按比例扣除
        BigDecimal absentDeduction = dailyRate.multiply(BigDecimal.valueOf(absentCount)).multiply(new BigDecimal("2.0")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal leaveDeduction = dailyRate.multiply(leaveDays).setScale(2, RoundingMode.HALF_UP);

        BigDecimal actualBase = baseSalary.subtract(absentDeduction).subtract(leaveDeduction);
        if (actualBase.compareTo(BigDecimal.ZERO) < 0) {
            actualBase = BigDecimal.ZERO;
        }

        // 最终实发工资：有效底薪 + 有效绩效 + 加班费
        BigDecimal actualSalary = actualBase.add(actualBonus).add(overtimePay).setScale(2, RoundingMode.HALF_UP);

        SalaryMonthlyStatement statement = new SalaryMonthlyStatement();
        statement.setUserId(userId);
        statement.setSettleMonth(settleMonth);
        statement.setBaseSalary(baseSalary);
        statement.setDailyWage(dailyRate.setScale(2, RoundingMode.HALF_UP));
        statement.setHourlyWage(hourlyRate.setScale(2, RoundingMode.HALF_UP));
        statement.setOvertimeHours(report.getOvertimeHours());
        statement.setOvertimePay(overtimePay);
        statement.setAllowance(allowance);
        statement.setPerformanceBonus(performanceBonus);
        statement.setMissingCardDeduction(missingCardDeduction);
        statement.setOffWorkHours(BigDecimal.valueOf(offWorkHours).setScale(2, RoundingMode.HALF_UP));
        statement.setLateDeduction(lateDeduction);
        statement.setAbsentDays(BigDecimal.valueOf(absentCount));
        statement.setAbsentDeduction(absentDeduction);
        statement.setLeaveDays(leaveDays);
        statement.setLeaveDeduction(leaveDeduction);
        statement.setActualSalary(actualSalary);
        statement.setStatus("DRAFT");
        
        salaryStatementMapper.upsertStatement(statement);
    }
}

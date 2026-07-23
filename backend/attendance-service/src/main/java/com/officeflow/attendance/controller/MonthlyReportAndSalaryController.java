package com.officeflow.attendance.controller;

import com.officeflow.attendance.entity.AttendanceMonthlyReport;
import com.officeflow.attendance.entity.SalaryMonthlyStatement;
import com.officeflow.attendance.service.MonthlyReportAndSalaryService;
import com.officeflow.attendance.util.RequestUser;
import com.officeflow.common.api.ApiResponse;
import com.officeflow.common.api.PageResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class MonthlyReportAndSalaryController {

    private final MonthlyReportAndSalaryService monthlyReportAndSalaryService;

    /**
     * 手动触发生成/更新月度考勤报表
     */
    @PostMapping("/monthly-reports/generate")
    public ApiResponse<Void> generateReport(@RequestParam(name = "month", required = false) String month) {
        String targetMonth = (month != null && !month.isBlank()) ? month : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        monthlyReportAndSalaryService.generateMonthlyReport(targetMonth);
        return ApiResponse.ok();
    }

    /**
     * 查询月度考勤报表列表
     */
    @GetMapping("/monthly-reports")
    public ApiResponse<PageResult<AttendanceMonthlyReport>> getMonthlyReports(
            @RequestParam(name = "month", required = false) String month,
            @RequestParam(name = "deptId", required = false) Long deptId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") long page,
            @RequestParam(name = "pageSize", defaultValue = "10") long pageSize) {
        String targetMonth = (month != null && !month.isBlank()) ? month : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return ApiResponse.ok(monthlyReportAndSalaryService.getMonthlyReports(targetMonth, deptId, keyword, page, pageSize));
    }

    /**
     * 手动触发生成/计算全员工资结算条
     */
    @PostMapping("/salary/generate")
    public ApiResponse<Void> generateSalary(@RequestParam(name = "month", required = false) String month) {
        String targetMonth = (month != null && !month.isBlank()) ? month : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        monthlyReportAndSalaryService.generateMonthlySalary(targetMonth);
        return ApiResponse.ok();
    }

    /**
     * 管理员/HR 分页查询全员工资结算列表
     */
    @GetMapping("/salary/statements")
    public ApiResponse<PageResult<SalaryMonthlyStatement>> getSalaryStatements(
            @RequestParam(name = "month", required = false) String month,
            @RequestParam(name = "deptId", required = false) Long deptId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") long page,
            @RequestParam(name = "pageSize", defaultValue = "10") long pageSize) {
        String targetMonth = (month != null && !month.isBlank()) ? month : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return ApiResponse.ok(monthlyReportAndSalaryService.getSalaryStatements(targetMonth, deptId, keyword, page, pageSize));
    }

    /**
     * 批量发布/发放工资单
     */
    @PostMapping("/salary/publish")
    public ApiResponse<Void> publishSalaryStatements(@RequestBody java.util.List<Long> ids, HttpServletRequest request) {
        String roles = RequestUser.roles(request);
        if (!roles.contains("ADMIN") && !roles.contains("MANAGER") && !roles.contains("HR")) {
            return ApiResponse.error(403, "无权限执行薪资发放操作");
        }
        monthlyReportAndSalaryService.publishSalaryStatements(ids);
        return ApiResponse.ok();
    }

    /**
     * 员工查询个人特定月份工资条
     */
    @GetMapping("/salary/my")
    public ApiResponse<SalaryMonthlyStatement> getMySalaryStatement(
            @RequestParam(name = "month", required = false) String month,
            HttpServletRequest httpRequest) {
        Long userId = RequestUser.userId(httpRequest);
        String targetMonth = (month != null && !month.isBlank()) ? month : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        return ApiResponse.ok(monthlyReportAndSalaryService.getMySalaryStatement(userId, targetMonth));
    }
}

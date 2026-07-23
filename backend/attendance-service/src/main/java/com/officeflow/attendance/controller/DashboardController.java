package com.officeflow.attendance.controller;

import com.officeflow.attendance.mapper.DashboardMapper;
import com.officeflow.attendance.util.RequestUser;
import com.officeflow.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据大屏聚合接口（按角色区分数据范围）
 * ADMIN → 全公司数据
 * MANAGER/EMPLOYEE → 本部门及子部门数据
 */
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardMapper dashboardMapper;

    /**
     * 大屏总览指标
     */
    @GetMapping("/dashboard/overview")
    public ApiResponse<Map<String, Object>> overview(HttpServletRequest httpRequest) {
        Long userId = RequestUser.userId(httpRequest);
        if (userId == null) userId = 0L;

        Map<String, Object> scope = getUserScope(userId);
        boolean isAdmin = "ALL".equals(scope.get("dataScope"));

        Map<String, Object> raw = isAdmin
                ? dashboardMapper.selectOverview(userId)
                : dashboardMapper.selectOverviewByDept(userId, (Long) scope.get("deptId"));

        Map<String, Object> result = new LinkedHashMap<>();
        long totalUsers = toLong(raw.get("totalUsers"));
        long todayCheckIn = toLong(raw.get("todayCheckIn"));

        result.put("totalUsers", totalUsers);
        result.put("todayCheckIn", todayCheckIn);
        result.put("todayLate", toLong(raw.get("todayLate")));
        result.put("todayEarly", toLong(raw.get("todayEarly")));
        result.put("todayAbsent", toLong(raw.get("todayAbsent")));
        result.put("pendingApprovals", toLong(raw.get("pendingApprovals")));
        result.put("noticeReadRate", toDouble(raw.get("noticeReadRate")));

        double rate = totalUsers > 0
                ? BigDecimal.valueOf(todayCheckIn * 100.0 / totalUsers).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        result.put("attendanceRate", rate);
        result.put("dataScope", isAdmin ? "ALL" : "DEPT");

        return ApiResponse.ok(result);
    }

    /**
     * 本周考勤趋势：全公司维度，所有角色可见
     */
    @GetMapping("/dashboard/weekly-trend")
    public ApiResponse<List<Map<String, Object>>> weeklyTrend() {
        List<Map<String, Object>> rows = dashboardMapper.selectWeeklyTrend();

        String[] dayLabels = {"周一", "周二", "周三", "周四", "周五"};
        Map<Integer, Map<String, Object>> byIndex = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            int idx = ((Number) r.get("dayIndex")).intValue();
            byIndex.put(idx, r);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> row = byIndex.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("day", dayLabels[i]);
            if (row != null) {
                item.put("normal", toLong(row.get("normal")));
                item.put("late", toLong(row.get("late")));
                item.put("earlyLeave", toLong(row.get("earlyLeave")));
                item.put("missingCard", toLong(row.get("missingCard")));
            } else {
                item.put("normal", 0L);
                item.put("late", 0L);
                item.put("earlyLeave", 0L);
                item.put("missingCard", 0L);
            }
            result.add(item);
        }
        return ApiResponse.ok(result);
    }

    /**
     * 部门周出勤热力图：ADMIN 看全公司，MANAGER/EMPLOYEE 看本部门及子部门
     */
    @GetMapping("/dashboard/dept-heatmap")
    public ApiResponse<List<Map<String, Object>>> deptHeatmap(HttpServletRequest httpRequest) {
        Long userId = RequestUser.userId(httpRequest);
        if (userId == null) userId = 0L;

        Map<String, Object> scope = getUserScope(userId);
        boolean isAdmin = "ALL".equals(scope.get("dataScope"));

        List<Map<String, Object>> rows = isAdmin
                ? dashboardMapper.selectDeptHeatmap()
                : dashboardMapper.selectDeptHeatmapByDept((Long) scope.get("deptId"));

        return ApiResponse.ok(buildHeatmapResult(rows));
    }

    /**
     * 审批类型分布：ADMIN 看全公司，MANAGER/EMPLOYEE 看本部门
     */
    @GetMapping("/dashboard/flow-distribution")
    public ApiResponse<List<Map<String, Object>>> flowDistribution(HttpServletRequest httpRequest) {
        Long userId = RequestUser.userId(httpRequest);
        if (userId == null) userId = 0L;

        Map<String, Object> scope = getUserScope(userId);
        boolean isAdmin = "ALL".equals(scope.get("dataScope"));

        List<Map<String, Object>> rows = isAdmin
                ? dashboardMapper.selectFlowDistribution()
                : dashboardMapper.selectFlowDistributionByDept((Long) scope.get("deptId"));

        Map<String, String> typeNames = Map.of(
                "LEAVE", "请假",
                "OVERTIME", "加班",
                "CORRECTION", "补卡"
        );

        List<Map<String, Object>> result = rows.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            String type = (String) row.get("applyType");
            item.put("type", type);
            item.put("name", typeNames.getOrDefault(type, type));
            item.put("count", toLong(row.get("count")));
            return item;
        }).collect(Collectors.toList());

        return ApiResponse.ok(result);
    }

    /* ── 工具方法 ── */

    private Map<String, Object> getUserScope(Long userId) {
        Map<String, Object> scope = dashboardMapper.selectUserScope(userId);
        if (scope == null) {
            scope = new HashMap<>();
            scope.put("dataScope", "SELF");
            scope.put("deptId", 0L);
        }
        if (scope.get("deptId") == null) {
            scope.put("deptId", 0L);
        }
        return scope;
    }

    private List<Map<String, Object>> buildHeatmapResult(List<Map<String, Object>> rows) {
        Map<Long, Map<String, Object>> deptMap = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            Long deptId = toLong(row.get("deptId"));
            Map<String, Object> dept = deptMap.computeIfAbsent(deptId, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("deptId", deptId);
                m.put("deptName", row.get("deptName"));
                m.put("total", toLong(row.get("total")));
                m.put("data", new long[7]);
                return m;
            });
            if (row.get("workDate") != null) {
                int dayIndex = ((Number) row.get("dayIndex")).intValue();
                int mapped = dayIndex == 0 ? 6 : dayIndex - 1;
                long[] data = (long[]) dept.get("data");
                data[mapped] = toLong(row.get("present"));
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> dept : deptMap.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", dept.get("deptName"));
            item.put("total", dept.get("total"));
            item.put("data", dept.get("data"));
            result.add(item);
        }
        return result;
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number) return ((Number) val).longValue();
        return 0L;
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0.0;
    }
}

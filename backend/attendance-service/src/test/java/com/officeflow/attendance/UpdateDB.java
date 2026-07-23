package com.officeflow.attendance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class UpdateDB {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3307/officeflow?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci&characterSetResults=utf8mb4&serverTimezone=Asia/Shanghai";
        String user = "root";
        String pass = "root";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(url, user, pass);
                 Statement stmt = conn.createStatement()) {
                
                String sql = "ALTER TABLE salary_monthly_statement " +
                        "ADD COLUMN daily_wage DECIMAL(10,2) COMMENT '日薪', " +
                        "ADD COLUMN hourly_wage DECIMAL(10,2) COMMENT '时薪', " +
                        "ADD COLUMN overtime_hours DECIMAL(5,1) COMMENT '加班小时数', " +
                        "ADD COLUMN off_work_hours DECIMAL(5,1) COMMENT '迟到/早退/缺卡折算扣除总小时数', " +
                        "ADD COLUMN absent_days DECIMAL(5,1) COMMENT '旷工天数', " +
                        "ADD COLUMN leave_days DECIMAL(5,1) COMMENT '请假天数'";
                
                stmt.executeUpdate(sql);
                System.out.println("ALTER TABLE SUCCESSFUL");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

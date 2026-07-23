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
                
                String query = "SELECT c.id, c.attendance_record_id, c.correction_type, c.correction_time, c.status as c_status, f.status as f_status, f.apply_type " +
                               "FROM attendance_correction_apply c " +
                               "LEFT JOIN flow_apply f ON c.flow_apply_id = f.id WHERE c.user_id = 4";
                java.sql.ResultSet rs = stmt.executeQuery(query);
                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    sb.append("ID: ").append(rs.getInt("id"))
                      .append(", RecID: ").append(rs.getString("attendance_record_id"))
                      .append(", Type: ").append(rs.getString("correction_type"))
                      .append(", C_Status: ").append(rs.getString("c_status"))
                      .append(", F_Status: ").append(rs.getString("f_status"))
                      .append("\n");
                }
                java.nio.file.Files.write(java.nio.file.Paths.get("D:\\newLand\\OfficeFlow\\db_output.txt"), sb.toString().getBytes());
                System.out.println("QUERY SUCCESSFUL");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

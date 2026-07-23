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
                
                String sqlPath = "D:\\newLand\\OfficeFlow\\docs\\sql\\init.sql";
                String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(sqlPath)));
                String[] queries = content.split(";");
                for (String query : queries) {
                    if (query.trim().length() > 0) {
                        stmt.executeUpdate(query);
                    }
                }
                System.out.println("SQL EXECUTION SUCCESSFUL");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

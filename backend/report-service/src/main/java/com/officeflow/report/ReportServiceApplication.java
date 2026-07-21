package com.officeflow.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.officeflow")
public class ReportServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReportServiceApplication.class, args);
    }
}

package com.officeflow.notice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.officeflow")
public class NoticeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NoticeServiceApplication.class, args);
    }
}

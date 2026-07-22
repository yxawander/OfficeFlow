package com.officeflow.notice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.officeflow")
@EnableFeignClients(basePackages = "com.officeflow.api")
public class NoticeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NoticeServiceApplication.class, args);
    }
}

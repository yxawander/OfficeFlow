package com.officeflow.flow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.officeflow")
@EnableFeignClients(basePackages = "com.officeflow.api")
@EnableScheduling
public class FlowServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlowServiceApplication.class, args);
    }
}

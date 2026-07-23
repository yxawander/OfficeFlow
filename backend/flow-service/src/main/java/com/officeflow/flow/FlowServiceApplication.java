package com.officeflow.flow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.officeflow")
@EnableFeignClients(basePackages = "com.officeflow.api")
public class FlowServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlowServiceApplication.class, args);
    }
}

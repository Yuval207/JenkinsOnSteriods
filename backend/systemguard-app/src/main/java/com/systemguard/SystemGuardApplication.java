package com.systemguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SystemGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemGuardApplication.class, args);
    }
}

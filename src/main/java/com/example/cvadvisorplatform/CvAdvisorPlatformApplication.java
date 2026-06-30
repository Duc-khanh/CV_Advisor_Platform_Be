package com.example.cvadvisorplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CvAdvisorPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CvAdvisorPlatformApplication.class, args);
    }

}

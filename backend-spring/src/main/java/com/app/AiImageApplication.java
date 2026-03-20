package com.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AiImageApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiImageApplication.class, args);
    }
}

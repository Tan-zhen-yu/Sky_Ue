package com.tzy.sky;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 必须开启此注解
public class SatelliteApplication {
    public static void main(String[] args) {
        SpringApplication.run(SatelliteApplication.class, args);
    }
}
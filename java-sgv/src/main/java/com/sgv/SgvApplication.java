package com.sgv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableMethodSecurity
public class SgvApplication {
    public static void main(String[] args) {
        String headless = System.getProperty("sgv.headless");
        if ("true".equalsIgnoreCase(headless)) {
            SpringApplication.run(SgvApplication.class, args);
        } else {
            com.sgv.desktop.MainApp.launch(com.sgv.desktop.MainApp.class, args);
        }
    }
}

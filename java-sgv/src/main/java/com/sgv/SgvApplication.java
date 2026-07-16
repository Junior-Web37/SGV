package com.sgv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
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

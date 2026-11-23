package com.sdjz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = "com.sdjz")
@Slf4j
@EnableTransactionManagement
public class novelApplication {
    public static void main(String[] args) {
        SpringApplication.run(novelApplication.class, args);
    }

}

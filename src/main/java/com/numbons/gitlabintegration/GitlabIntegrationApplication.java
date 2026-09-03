package com.numbons.gitlabintegration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableCaching
public class GitlabIntegrationApplication {
    public static void main(String[] args) {
        SpringApplication.run(GitlabIntegrationApplication.class, args);
    }
}

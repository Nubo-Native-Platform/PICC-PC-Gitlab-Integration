package com.numbons.gitlabintegration.api.configuration;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/*@Configuration*/
@Slf4j
public class GitLabClientConfiguration {
    @Value("${feign.apiToken}")
    private String apiToken;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> requestTemplate.header("PRIVATE-TOKEN", apiToken);
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new GitLabClientErrorDecoder();
    }
    
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}

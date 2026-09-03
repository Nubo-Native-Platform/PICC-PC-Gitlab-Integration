
package com.numbons.gitlabintegration.api.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

/* @Configuration */
@Slf4j
public class NnpGitLabClientConfiguration {
	@Value("${nnp.feign.apiToken}")
    private String apiToken;

    @Bean
    public RequestInterceptor requestInterceptorNnp() {
        return requestTemplate -> requestTemplate.header("PRIVATE-TOKEN", apiToken);
    }

    @Bean
    public ErrorDecoder errorDecoderNnp() {
        return new GitLabClientErrorDecoder();
    }
    
    @Bean
    Logger.Level feignLoggerLevelNnp() {
        return Logger.Level.FULL;
    }

}

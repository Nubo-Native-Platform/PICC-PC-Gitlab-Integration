package com.numbons.gitlabintegration.api.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 / Swagger UI Configuration for PICC-PC-Gitlab-Integration.
 */
@Configuration
public class OpenAPIConfiguration {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PICC-PC-Gitlab-Integration REST API")
                        .description("Enterprise REST API integration microservice for GitLab REST API v4. "
                                + "Provides automated workspace provisioning, group/subgroup governance, "
                                + "project lifecycle automation, scoped PAT management, and in-memory caching "
                                + "for the Nubo Native Platform (NNP).")
                        .version("0.0.1-SNAPSHOT")
                        .contact(new Contact()
                                .name("Nubo Native Platform Team")
                                .email("contribution@nubons.com")
                                .url("https://github.com/Nubo-Native-Platform/PICC-PC-Gitlab-Integration"))
                        .license(new License()
                                .name("Apache License 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("/").description("Current Server Context"),
                        new Server().url("http://localhost:" + serverPort).description("Local Development Server")
                ));
    }
}

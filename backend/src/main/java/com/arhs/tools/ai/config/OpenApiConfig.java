package com.arhs.tools.ai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI registryOfficeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Registry Office API")
                        .description("REST API for managing persons in the civil registry")
                        .version("1.0.0"));
    }
}


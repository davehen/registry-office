package com.arhs.tools.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfiguration implements WebMvcConfigurer {
    private final CorsProperties properties;
    public CorsConfiguration(CorsProperties properties) { this.properties = properties; }
    @Override public void addCorsMappings(CorsRegistry registry) {
        for (String path : new String[]{"/person", "/person/**", "/duplicate-cases", "/duplicate-cases/**",
                "/person-merges/**", "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"}) {
            registry.addMapping(path).allowedOrigins(properties.allowedOrigins().toArray(String[]::new))
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("Content-Type").exposedHeaders("Location").allowCredentials(false);
        }
    }
}

package com.arhs.tools.ai.config;

import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins.stream()
                .map(String::trim).filter(origin -> !origin.isEmpty()).distinct().toList();
        for (String origin : allowedOrigins) {
            URI uri = URI.create(origin);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawUserInfo() != null
                    || !uri.getRawPath().isEmpty() || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw new IllegalArgumentException("CORS origins must be exact HTTP(S) origins without paths or wildcards");
            }
        }
    }
}

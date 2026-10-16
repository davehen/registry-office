package com.arhs.tools.ai.config;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class CorsPropertiesTest {
    @Test void explicitOriginsOnly() {
        for (String invalid : List.of("*", "https://*.example.test", "https://example.test/", "https://example.test/path", "file:///tmp", "https://user@example.test", "https://example.test?q=1"))
            assertThatThrownBy(() -> new CorsProperties(List.of(invalid))).isInstanceOf(IllegalArgumentException.class);
        assertThat(new CorsProperties(List.of(" https://example.test ", "https://example.test")).allowedOrigins()).containsExactly("https://example.test");
        assertThat(new CorsProperties(null).allowedOrigins()).isEmpty();
    }
}

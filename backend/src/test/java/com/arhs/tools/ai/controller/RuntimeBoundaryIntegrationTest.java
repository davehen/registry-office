package com.arhs.tools.ai.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.cors.allowed-origins=https://ui.example.test,https://second.example.test",
        "spring.datasource.url=jdbc:h2:mem:runtime-boundary;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class RuntimeBoundaryIntegrationTest {
    @Autowired MockMvc mvc;
    @ParameterizedTest @ValueSource(strings = {"/", "/index.html", "/app.js", "/style.css", "/unknown-ui-route"})
    void backendDoesNotServeFrontend(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isNotFound()).andExpect(forwardedUrl(null));
    }
    @ParameterizedTest @ValueSource(strings = {"https://ui.example.test", "https://second.example.test"})
    void approvedOriginCanReadApi(String origin) throws Exception {
        mvc.perform(get("/person").header("Origin", origin)).andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }
    @ParameterizedTest @ValueSource(strings = {"POST", "PUT", "DELETE"})
    void requiredWriteMethodsPreflight(String method) throws Exception {
        mvc.perform(options("/person").header("Origin", "https://ui.example.test")
                .header("Access-Control-Request-Method", method).header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "https://ui.example.test"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Content-Type"));
    }
    @Test void foreignOriginAndUnsupportedHeaderAreDenied() throws Exception {
        mvc.perform(get("/person").header("Origin", "https://foreign.example.test"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mvc.perform(options("/person").header("Origin", "https://ui.example.test")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isForbidden());
    }
    @Test void locationIsExposedOnCreateAndOpenApiRemainsAvailable() throws Exception {
        mvc.perform(post("/person").header("Origin", "https://ui.example.test").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userName\":\"cors-test\",\"firstName\":\"First\",\"lastName\":\"Last\",\"email\":\"cors@example.test\",\"birthDate\":\"1990-01-01\"}"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(header().string("Access-Control-Expose-Headers", "Location"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.openapi").exists());
    }
    @Test void duplicateEndpointsShareCorsBoundary() throws Exception {
        mvc.perform(get("/duplicate-cases").header("Origin", "https://ui.example.test"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "https://ui.example.test"));
        mvc.perform(options("/duplicate-cases/1/merge").header("Origin", "https://ui.example.test")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk());
    }
}

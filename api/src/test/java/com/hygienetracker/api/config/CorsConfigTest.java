package com.hygienetracker.api.config;

import com.hygienetracker.api.controller.HealthController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The browser preflight succeeds only for origins listed in CORS_ALLOWED_ORIGINS. */
@WebMvcTest(value = HealthController.class,
        properties = "app.cors.allowed-origins=https://web.example.com,http://localhost:8081")
@Import(SecurityConfig.class)
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    private static MockHttpServletRequestBuilder preflight(String origin) {
        return options("/api/auth/google")
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type");
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://web.example.com", "http://localhost:8081"})
    void allowsEveryConfiguredOrigin(String origin) throws Exception {
        mockMvc.perform(preflight(origin))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
    }

    @Test
    void rejectsOtherOrigins() throws Exception {
        mockMvc.perform(preflight("https://evil.example.com"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}

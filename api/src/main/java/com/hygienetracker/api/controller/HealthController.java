package com.hygienetracker.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "message", "Hygiene Tracker API is running"
        );
    }

    /** Protected diagnostic endpoint used by the running app to verify its JWT connection. */
    @GetMapping("/connection-test")
    public Map<String, Object> connectionTest(Authentication authentication) {
        return Map.of(
                "status", "AUTHENTICATED",
                "subject", authentication.getName(),
                "authenticated", authentication.isAuthenticated()
        );
    }
}

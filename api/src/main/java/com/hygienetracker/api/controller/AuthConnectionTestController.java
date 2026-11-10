package com.hygienetracker.api.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/connection-test")
@CrossOrigin(origins = "*")
public class AuthConnectionTestController {

    @GetMapping
    public ConnectionTestResponse connectionTest() {
        return new ConnectionTestResponse(
                true,
                "mock",
                "Spring Boot connection is working",
                new MockUser("mock-github-user", "octocat", "Octocat", "octocat@example.com"));
    }

    public record ConnectionTestResponse(
            boolean valid,
            String provider,
            String message,
            MockUser user) {}

    public record MockUser(
            String id,
            String login,
            String name,
            String email) {}
}

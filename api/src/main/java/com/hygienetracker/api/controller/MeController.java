package com.hygienetracker.api.controller;

import com.hygienetracker.api.service.Stats;
import com.hygienetracker.api.service.StatsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints about the signed-in user ({@code /api/me} in {@code docs/openapi.yaml}). The user is
 * always the JWT's subject, never an id from the request, so a user can only read their own data.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final StatsService statsService;

    public MeController(StatsService statsService) {
        this.statsService = statsService;
    }

    /**
     * {@code GET /api/me/stats}: the signed-in user's streak, score and missed days (PRD-5).
     * Requests without a valid token get 401 from {@code SecurityConfig} before reaching here.
     *
     * @param jwt the signed-in user's token; its subject is their user id
     * @return the user's stats
     */
    @GetMapping("/stats")
    public Stats myStats(@AuthenticationPrincipal Jwt jwt) {
        return statsService.statsFor(Long.parseLong(jwt.getSubject()));
    }
}

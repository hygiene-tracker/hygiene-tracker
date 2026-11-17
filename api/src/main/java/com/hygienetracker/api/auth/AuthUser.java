package com.hygienetracker.api.auth;

import java.time.Instant;

import com.hygienetracker.api.entity.User;

/**
 * The signed-in user as returned by the sign-in endpoints (the {@code User} schema in
 * docs/openapi.yaml). {@code displayName} and {@code avatar} stay {@code null} until the user sets
 * them (PRD-2); they are never filled from the provider, so real names stay off the leaderboard.
 */
public record AuthUser(
        long id,
        String email,
        String displayName,
        String avatar,
        String role,
        Instant createdAt) {

    /** A user who has just signed in for the first time, with the default {@code USER} role. */
    public static AuthUser newUser(long id, String email) {
        return new AuthUser(id, email, null, null, "USER", Instant.now());
    }

    public static AuthUser from(User user) {
        Instant createdAt = user.getCreatedAt() == null ? Instant.now() : user.getCreatedAt();
        return new AuthUser(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatar(),
                user.getRole(),
                createdAt);
    }
}

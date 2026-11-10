package com.hygienetracker.api.auth;

/**
 * The body of a successful sign-in ({@code AuthResponse} in docs/openapi.yaml).
 *
 * @param token     the API's JWT, sent as {@code Authorization: Bearer <token>} on later requests
 * @param tokenType always {@code Bearer}
 * @param expiresIn seconds until the token expires
 * @param user      the signed-in user
 */
public record AuthResponse(String token, String tokenType, long expiresIn, AuthUser user) {}

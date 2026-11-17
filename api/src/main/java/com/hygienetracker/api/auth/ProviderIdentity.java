package com.hygienetracker.api.auth;

/** Verified identity data returned by an OAuth provider. */
public record ProviderIdentity(
        String provider,
        String providerUserId,
        String email,
        boolean emailVerified) {}

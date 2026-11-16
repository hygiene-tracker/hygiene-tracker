package com.hygienetracker.api.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record AppJwtProperties(String issuer, String secretBase64, Duration ttl) {}

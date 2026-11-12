package com.hygienetracker.api.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Signs and checks the API's own JWTs (Appendix I, 3.4). One HMAC key, read from
 * {@code JWT_SECRET}, is used for both, so every sign-in provider produces the same kind of token.
 */
@Configuration
public class JwtConfig {

    /** The {@code iss} claim on every token this API issues; tokens with any other issuer are rejected. */
    public static final String ISSUER = "hygiene-tracker-api";

    private static final int MIN_SECRET_BYTES = 32;

    @Bean
    SecretKey jwtSigningKey(@Value("${jwt.secret}") String secret) {
        return signingKey(secret);
    }

    /** Creates the API's tokens. */
    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    /** Checks the signature, expiry and issuer of tokens sent to protected endpoints. */
    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }

    /**
     * Turns the configured secret into an HMAC-SHA256 key.
     *
     * @throws IllegalStateException if the secret is shorter than 32 bytes, so a weak key stops
     *                               the API from starting instead of signing weak tokens
     */
    public static SecretKey signingKey(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}

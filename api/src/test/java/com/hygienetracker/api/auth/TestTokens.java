package com.hygienetracker.api.auth;

import javax.crypto.SecretKey;

import com.hygienetracker.api.config.JwtConfig;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Builds the API's token service and decoder for tests that don't start Spring. */
public final class TestTokens {

    public static final String SECRET = "test-only-secret-at-least-32-characters-long";

    private TestTokens() {
    }

    /** A token service signing with {@link #SECRET}. */
    public static AppTokenService service() {
        return service(SECRET);
    }

    /** A token service signing with the given secret. */
    public static AppTokenService service(String secret) {
        return new AppTokenService(new NimbusJwtEncoder(new ImmutableSecret<>(JwtConfig.signingKey(secret))));
    }

    /** A decoder that checks tokens exactly like the running API does. */
    public static JwtDecoder decoder() {
        SecretKey key = JwtConfig.signingKey(SECRET);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtConfig.ISSUER));
        return decoder;
    }
}

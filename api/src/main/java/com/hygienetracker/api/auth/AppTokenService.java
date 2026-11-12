package com.hygienetracker.api.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.hygienetracker.api.config.JwtConfig;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues the API's own JWT after a successful sign-in (Appendix I, 3.4). Every provider uses this
 * one service, so Google and GitHub users get identical tokens and every endpoint treats them
 * the same.
 */
@Service
public class AppTokenService {

    /** How long a token is valid. */
    public static final Duration TOKEN_LIFETIME = Duration.ofHours(1);

    /** The claim holding the user's roles, read by {@code SecurityConfig} for {@code hasRole(...)}. */
    public static final String ROLES_CLAIM = "roles";

    private final JwtEncoder encoder;

    public AppTokenService(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    /** Signs a one-hour token for the user and wraps it in the sign-in response. */
    public AuthResponse signIn(AuthUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_LIFETIME))
                .subject(String.valueOf(user.id()))
                .claim("email", user.email())
                .claim(ROLES_CLAIM, List.of(user.role()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", TOKEN_LIFETIME.toSeconds(), user);
    }
}

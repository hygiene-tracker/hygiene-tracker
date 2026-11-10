package com.hygienetracker.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import com.hygienetracker.api.config.JwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

/** The API's own tokens: what they contain, and which tokens the API refuses. */
class AppTokenServiceTest {

    private final AppTokenService tokens = TestTokens.service();

    @Test
    void issuedTokenCarriesUserIdRolesIssuerAndOneHourExpiry() {
        AuthResponse response = tokens.signIn(AuthUser.newUser(7, "student@example.com"));

        Jwt jwt = TestTokens.decoder().decode(response.token());

        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsStringList(AppTokenService.ROLES_CLAIM)).containsExactly("USER");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("student@example.com");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(JwtConfig.ISSUER);
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600);
    }

    @Test
    void newUserHasNoDisplayNameOrAvatarAndTheUserRole() {
        AuthUser user = AuthUser.newUser(7, "student@example.com");

        assertThat(user.displayName()).isNull();
        assertThat(user.avatar()).isNull();
        assertThat(user.role()).isEqualTo("USER");
    }

    @Test
    void rejectsTokenSignedWithADifferentSecret() {
        AppTokenService otherKey = TestTokens.service("a-completely-different-secret-of-32-plus-bytes");
        String forged = otherKey.signIn(AuthUser.newUser(7, "student@example.com")).token();

        assertThatThrownBy(() -> TestTokens.decoder().decode(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTamperedToken() {
        String token = tokens.signIn(AuthUser.newUser(7, "student@example.com")).token();
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "x." + parts[2];

        assertThatThrownBy(() -> TestTokens.decoder().decode(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void refusesSecretShorterThan32Bytes() {
        assertThatThrownBy(() -> JwtConfig.signingKey("too-short"))
                .isInstanceOf(IllegalStateException.class);
    }
}

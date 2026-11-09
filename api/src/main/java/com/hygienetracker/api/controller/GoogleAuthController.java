package com.hygienetracker.api.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class GoogleAuthController {

    private static final long TOKEN_LIFETIME_SECONDS = 3_600;

    private final GoogleIdTokenVerifier verifier;
    private final byte[] jwtSecret;

    @Autowired
    public GoogleAuthController(
            @Value("${google.oauth.client-ids:}") String clientIds,
            @Value("${jwt.secret:}") String jwtSecret) {
        List<String> audiences = Arrays.stream(clientIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .toList();
        this.verifier = audiences.isEmpty()
                ? null
                : new GoogleIdTokenVerifier.Builder(
                        new NetHttpTransport(), GsonFactory.getDefaultInstance())
                        .setAudience(audiences)
                        .build();
        this.jwtSecret = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    GoogleAuthController(GoogleIdTokenVerifier verifier, String jwtSecret) {
        this.verifier = verifier;
        this.jwtSecret = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @PostMapping("/google")
    public ResponseEntity<?> signInWithGoogle(@Valid @RequestBody TokenRequest request) {
        if (request.idToken() == null || request.idToken().isBlank()) {
            return problem(HttpStatus.BAD_REQUEST, "idToken is required");
        }
        if (verifier == null || jwtSecret.length < 32) {
            return problem(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not configured");
        }

        try {
            GoogleIdToken token = verifier.verify(request.idToken());
            if (token == null) {
                return problem(HttpStatus.BAD_REQUEST, "Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = token.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                return problem(HttpStatus.BAD_REQUEST, "Google account email is not verified");
            }

            InstantAndToken applicationToken = createApplicationToken(payload);
            UserResponse user = new UserResponse(
                    stableUserId(payload.getSubject()),
                    payload.getEmail(),
                    claimAsString(payload, "name"),
                    null,
                    "USER",
                    applicationToken.createdAt());
            return ResponseEntity.ok(new AuthResponse(
                    applicationToken.token(),
                    "Bearer",
                    TOKEN_LIFETIME_SECONDS,
                    user));
        } catch (GeneralSecurityException | IOException exception) {
            return problem(HttpStatus.SERVICE_UNAVAILABLE, "Unable to verify Google ID token right now");
        } catch (JOSEException exception) {
            return problem(HttpStatus.SERVICE_UNAVAILABLE, "Unable to create an application token right now");
        }
    }

    private InstantAndToken createApplicationToken(GoogleIdToken.Payload payload) throws JOSEException {
        Instant createdAt = Instant.now();
        Date issuedAt = Date.from(createdAt);
        Date expiresAt = new Date(issuedAt.getTime() + TOKEN_LIFETIME_SECONDS * 1_000);
        String subject = payload.getSubject();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer("hygiene-tracker-api")
                .claim("email", payload.getEmail())
                .claim("role", "USER")
                .issueTime(issuedAt)
                .expirationTime(expiresAt)
                .jwtID(UUID.randomUUID().toString())
                .build();
        SignedJWT signedJwt = new SignedJWT(
                new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256), claims);
        signedJwt.sign(new MACSigner(jwtSecret));
        return new InstantAndToken(signedJwt.serialize(), createdAt);
    }

    private static long stableUserId(String subject) {
        long id = UUID.nameUUIDFromBytes(subject.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .getMostSignificantBits() & Long.MAX_VALUE;
        return id == 0 ? 1 : id;
    }

    private static ResponseEntity<Problem> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(new Problem(
                "about:blank",
                status.getReasonPhrase(),
                status.value(),
                detail,
                "/api/auth/google",
                UUID.randomUUID().toString(),
                null));
    }

    private static String claimAsString(GoogleIdToken.Payload payload, String claim) {
        Object value = payload.get(claim);
        return value instanceof String string ? string : null;
    }

    public record TokenRequest(
            @Size(max = 8192) String idToken) {}

    public record AuthResponse(
            String token,
            String tokenType,
            long expiresIn,
            UserResponse user) {}

    public record UserResponse(
            long id,
            String email,
            String displayName,
            String avatar,
            String role,
            Instant createdAt) {}

    public record Problem(
            String type,
            String title,
            int status,
            String detail,
            String instance,
            String requestId,
            List<FieldError> errors) {}

    public record FieldError(String field, String message) {}

    private record InstantAndToken(String token, Instant createdAt) {}
}

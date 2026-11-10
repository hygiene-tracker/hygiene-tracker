package com.hygienetracker.api.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.hygienetracker.api.auth.AppTokenService;
import com.hygienetracker.api.auth.AuthUser;
import com.hygienetracker.api.error.Problem;
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

/**
 * {@code POST /api/auth/google}: verifies a Google ID token and returns the API's own JWT
 * (Appendix I, 3.4).
 */
@RestController
@RequestMapping("/api/auth")
public class GoogleAuthController {

    private static final String PATH = "/api/auth/google";

    private final GoogleIdTokenVerifier verifier;
    private final AppTokenService tokens;

    @Autowired
    public GoogleAuthController(
            @Value("${google.oauth.client-ids:}") String clientIds,
            AppTokenService tokens) {
        List<String> audiences = Arrays.stream(clientIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .toList();
        this.verifier = audiences.isEmpty()
                ? null
                : new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                        .setAudience(audiences)
                        .build();
        this.tokens = tokens;
    }

    GoogleAuthController(GoogleIdTokenVerifier verifier, AppTokenService tokens) {
        this.verifier = verifier;
        this.tokens = tokens;
    }

    /** Signs in with a Google ID token: 200 with a JWT, or 400 / 503 as a {@link Problem}. */
    @PostMapping("/google")
    public ResponseEntity<?> signInWithGoogle(@Valid @RequestBody TokenRequest request) {
        if (request.idToken() == null || request.idToken().isBlank()) {
            return Problem.response(HttpStatus.BAD_REQUEST, "idToken is required", PATH);
        }
        if (verifier == null) {
            return Problem.response(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not configured", PATH);
        }

        try {
            GoogleIdToken token = verifier.verify(request.idToken());
            if (token == null) {
                return Problem.response(HttpStatus.BAD_REQUEST, "Invalid Google ID token", PATH);
            }

            GoogleIdToken.Payload payload = token.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                return Problem.response(HttpStatus.BAD_REQUEST, "Google account email is not verified", PATH);
            }

            // TODO: replace with the stored user from find-or-create once users are saved.
            AuthUser user = AuthUser.newUser(placeholderUserId(payload.getSubject()), payload.getEmail());
            return ResponseEntity.ok(tokens.signIn(user));
        } catch (GeneralSecurityException | IOException exception) {
            return Problem.response(
                    HttpStatus.SERVICE_UNAVAILABLE, "Unable to verify Google ID token right now", PATH);
        }
    }

    /** A stable stand-in ID derived from Google's subject until users are stored in the database. */
    private static long placeholderUserId(String subject) {
        long id = UUID.nameUUIDFromBytes(subject.getBytes(StandardCharsets.UTF_8))
                .getMostSignificantBits() & Long.MAX_VALUE;
        return id == 0 ? 1 : id;
    }

    /** The Android sign-in request: a Google ID token. */
    public record TokenRequest(@Size(max = 8192) String idToken) {}
}

package com.hygienetracker.api.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class GoogleAuthController {

    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthController(@Value("${google.oauth.client-ids:}") String clientIds) {
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
    }

    @PostMapping("/google")
    public ResponseEntity<?> validateGoogleToken(@Valid @RequestBody TokenRequest request) {
        if (verifier == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Google sign-in is not configured"));
        }

        try {
            GoogleIdToken token = verifier.verify(request.idToken());
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid Google ID token"));
            }

            GoogleIdToken.Payload payload = token.getPayload();
            VerifiedUser user = new VerifiedUser(
                    payload.getSubject(),
                    payload.getEmail(),
                    payload.getEmailVerified(),
                    claimAsString(payload, "name"),
                    claimAsString(payload, "picture"));
            return ResponseEntity.ok(Map.of("valid", true, "user", user));
        } catch (GeneralSecurityException | IOException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Unable to verify Google ID token right now"));
        }
    }

    private static String claimAsString(GoogleIdToken.Payload payload, String claim) {
        Object value = payload.get(claim);
        return value instanceof String string ? string : null;
    }

    public record TokenRequest(
            @NotBlank @Size(max = 8192) String idToken) {}

    public record VerifiedUser(
            String subject,
            String email,
            Boolean emailVerified,
            String name,
            String picture) {}
}

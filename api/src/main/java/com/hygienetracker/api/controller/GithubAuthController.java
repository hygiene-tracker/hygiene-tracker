package com.hygienetracker.api.controller;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class GithubAuthController {

    private static final long TOKEN_LIFETIME_SECONDS = 3_600;

    private final RestClient githubClient;
    private final RestClient oauthClient;
    private final String clientId;
    private final String clientSecret;
    private final byte[] jwtSecret;

    @Autowired
    public GithubAuthController(
            RestClient.Builder restClientBuilder,
            @Value("${github.oauth.client-id:}") String clientId,
            @Value("${github.oauth.client-secret:}") String clientSecret,
            @Value("${jwt.secret:}") String jwtSecret) {
        this(
                restClientBuilder
                        .baseUrl("https://api.github.com")
                        .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                        .defaultHeader(HttpHeaders.USER_AGENT, "hygiene-tracker-api")
                        .build(),
                restClientBuilder
                        .baseUrl("https://github.com")
                        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader(HttpHeaders.USER_AGENT, "hygiene-tracker-api")
                        .build(),
                clientId,
                clientSecret,
                jwtSecret);
    }

    GithubAuthController(
            RestClient githubClient,
            RestClient oauthClient,
            String clientId,
            String clientSecret,
            String jwtSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.jwtSecret = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.githubClient = githubClient;
        this.oauthClient = oauthClient;
    }

    @PostMapping("/github/exchange")
    public ResponseEntity<?> exchangeGithubCode(@Valid @RequestBody CodeRequest request) {
        if (clientId.isBlank() || clientSecret.isBlank() || jwtSecret.length < 32) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "GitHub sign-in is not configured"));
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            form.add("code", request.code());
            form.add("redirect_uri", request.redirectUri());
            form.add("code_verifier", request.codeVerifier());
            GithubToken token = oauthClient.post()
                    .uri("/login/oauth/access_token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(GithubToken.class);
            if (token == null || token.access_token() == null || token.access_token().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "GitHub authorization code could not be exchanged"));
            }

            GithubUser user = fetchGithubUser(token.access_token());
            if (user == null || user.id() == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "GitHub profile could not be loaded"));
            }
            if (!user.emailVerified() || user.email() == null || user.email().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "GitHub account email is not verified"));
            }
            return ResponseEntity.ok(createAuthResponse(user));
        } catch (RestClientResponseException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "GitHub authorization code is invalid or expired"));
        } catch (JOSEException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Unable to create an application token right now"));
        } catch (RestClientException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Unable to reach GitHub right now"));
        }
    }

    private AuthResponse createAuthResponse(GithubUser user) throws JOSEException {
        Instant createdAt = Instant.now();
        Date issuedAt = Date.from(createdAt);
        Date expiresAt = new Date(issuedAt.getTime() + TOKEN_LIFETIME_SECONDS * 1_000);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(user.id()))
                .issuer("hygiene-tracker-api")
                .claim("provider", "github")
                .claim("email", user.email())
                .claim("role", "USER")
                .issueTime(issuedAt)
                .expirationTime(expiresAt)
                .jwtID(UUID.randomUUID().toString())
                .build();
        SignedJWT signedJwt = new SignedJWT(
                new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256), claims);
        signedJwt.sign(new MACSigner(jwtSecret));
        return new AuthResponse(
                signedJwt.serialize(),
                "Bearer",
                TOKEN_LIFETIME_SECONDS,
                new UserResponse(
                        user.id(),
                        user.email(),
                        user.avatar_url(),
                        "USER",
                        createdAt));
    }

    private GithubUser fetchGithubUser(String accessToken) {
        GithubUser profile = githubClient.get()
                .uri("/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(GithubUser.class);
        if (profile == null || profile.id() == null) {
            return null;
        }

        GithubEmail[] emails = githubClient.get()
                .uri("/user/emails?per_page=100")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(GithubEmail[].class);
        String verifiedEmail = selectVerifiedEmail(emails);
        return new GithubUser(
                profile.id(), profile.login(), profile.name(),
                verifiedEmail, verifiedEmail != null, profile.avatar_url());
    }

    private static String selectVerifiedEmail(GithubEmail[] emails) {
        if (emails == null) {
            return null;
        }
        for (GithubEmail email : emails) {
            if (email != null && email.primary() && email.verified()) {
                return email.email();
            }
        }
        for (GithubEmail email : emails) {
            if (email != null && email.verified()) {
                return email.email();
            }
        }
        return null;
    }

    public record GithubToken(String access_token) {}

    public record GithubEmail(String email, boolean primary, boolean verified) {}

    public record CodeRequest(
            @NotBlank @Size(max = 4096) String code,
            @NotBlank @Size(max = 256) String codeVerifier,
            @NotBlank @Size(max = 512) String redirectUri) {}

    public record AuthResponse(
            String token,
            String tokenType,
            long expiresIn,
            UserResponse user) {}

    public record UserResponse(
            long id,
            String email,
            String avatar,
            String role,
            Instant createdAt) {}

    public record GithubUser(
            Long id,
            String login,
            String name,
            String email,
            boolean emailVerified,
            String avatar_url) {}
}

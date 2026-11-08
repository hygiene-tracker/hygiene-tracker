package com.hygienetracker.api.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class GithubAuthController {

    private final RestClient githubClient;
    private final RestClient oauthClient;
    private final String clientId;
    private final String clientSecret;
    private final String callbackUri;
    private final String appRedirectUri;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ConcurrentHashMap<String, Handoff> HANDOFFS = new ConcurrentHashMap<>();

    public GithubAuthController(
            RestClient.Builder restClientBuilder,
            @Value("${github.oauth.client-id:}") String clientId,
            @Value("${github.oauth.client-secret:}") String clientSecret,
            @Value("${github.oauth.callback-uri:http://localhost:8080/api/auth/github/callback}") String callbackUri,
            @Value("${github.oauth.app-redirect-uri:hygiene-tracker://auth/github}") String appRedirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.callbackUri = callbackUri;
        this.appRedirectUri = appRedirectUri;
        this.githubClient = restClientBuilder
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader(HttpHeaders.USER_AGENT, "hygiene-tracker-api")
                .build();
        this.oauthClient = restClientBuilder
                .baseUrl("https://github.com")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "hygiene-tracker-api")
                .build();
    }

    @GetMapping("/github/login")
    public ResponseEntity<Void> startGithubLogin(HttpSession session) {
        if (clientId.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        byte[] randomState = new byte[32];
        RANDOM.nextBytes(randomState);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(randomState);
        session.setAttribute("github_oauth_state", state);

        String authorizationUrl = UriComponentsBuilder
                .fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", callbackUri)
                .queryParam("scope", "read:user user:email")
                .queryParam("state", state)
                .build().encode().toUriString();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(java.net.URI.create(authorizationUrl))
                .build();
    }

    @GetMapping("/github/callback")
    public ResponseEntity<?> githubCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpSession session) {
        Object expectedState = session.getAttribute("github_oauth_state");
        session.removeAttribute("github_oauth_state");
        if (error != null) {
            return redirectToApp("github_denied", null);
        }
        if (expectedState == null || state == null || !expectedState.equals(state) || code == null || code.isBlank()) {
            return redirectToApp("invalid_callback", null);
        }
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return redirectToApp("github_not_configured", null);
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            form.add("code", code);
            form.add("redirect_uri", callbackUri);
            GithubToken token = oauthClient.post()
                    .uri("/login/oauth/access_token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(GithubToken.class);
            if (token == null || token.access_token() == null || token.access_token().isBlank()) {
                return redirectToApp("token_exchange_failed", null);
            }

            GithubUser user = fetchGithubUser(token.access_token());
            if (user == null || user.id() == null) {
                return redirectToApp("profile_lookup_failed", null);
            }
            HANDOFFS.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(Instant.now()));
            String handoffId = randomUrlSafeValue();
            HANDOFFS.put(handoffId, new Handoff(user, Instant.now().plusSeconds(120)));
            return redirectToApp(null, handoffId);
        } catch (RestClientException exception) {
            return redirectToApp("github_request_failed", null);
        }
    }

    @PostMapping("/github/result")
    public ResponseEntity<?> consumeGithubHandoff(@Valid @RequestBody HandoffRequest request) {
        Handoff handoff = HANDOFFS.remove(request.handoffId());
        if (handoff == null || handoff.expiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of("error", "GitHub sign-in result expired or was already used"));
        }
        return ResponseEntity.ok(Map.of("valid", true, "user", handoff.user()));
    }

    private ResponseEntity<Void> redirectToApp(String error, String handoffId) {
        UriComponentsBuilder redirect = UriComponentsBuilder.fromUriString(appRedirectUri);
        if (error != null) {
            redirect.queryParam("error", error);
        }
        if (handoffId != null) {
            redirect.queryParam("handoffId", handoffId);
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirect.build().encode().toUri())
                .build();
    }

    private static String randomUrlSafeValue() {
        byte[] randomValue = new byte[32];
        RANDOM.nextBytes(randomValue);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomValue);
    }

    @PostMapping("/github")
    public ResponseEntity<?> validateGithubToken(@Valid @RequestBody TokenRequest request) {
        try {
            GithubUser user = fetchGithubUser(request.accessToken());

            if (user == null || user.id() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid GitHub access token"));
            }
            return ResponseEntity.ok(Map.of("valid", true, "user", user));
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid GitHub access token"));
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "GitHub could not validate the access token"));
        } catch (RestClientException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Unable to reach GitHub right now"));
        }
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

    public record TokenRequest(
            @NotBlank @Size(max = 4096) String accessToken) {}

    public record GithubToken(String access_token) {}

    public record GithubEmail(String email, boolean primary, boolean verified) {}

    public record HandoffRequest(@NotBlank @Size(max = 128) String handoffId) {}

    private record Handoff(GithubUser user, Instant expiresAt) {}

    public record GithubUser(
            Long id,
            String login,
            String name,
            String email,
            boolean emailVerified,
            String avatar_url) {}
}

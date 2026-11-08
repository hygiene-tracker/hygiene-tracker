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
import java.util.Base64;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class GithubAuthController {

    private final RestClient githubClient;
    private final RestClient oauthClient;
    private final String clientId;
    private final String clientSecret;
    private final String callbackUri;
    private static final SecureRandom RANDOM = new SecureRandom();

    public GithubAuthController(
            RestClient.Builder restClientBuilder,
            @Value("${github.oauth.client-id:}") String clientId,
            @Value("${github.oauth.client-secret:}") String clientSecret,
            @Value("${github.oauth.callback-uri:http://localhost:8080/api/auth/github/callback}") String callbackUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.callbackUri = callbackUri;
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "GitHub sign-in was not approved"));
        }
        if (expectedState == null || state == null || !expectedState.equals(state) || code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid GitHub OAuth callback"));
        }
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "GitHub OAuth is not configured"));
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "GitHub did not issue an access token"));
            }

            GithubUser user = githubClient.get()
                    .uri("/user")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.access_token())
                    .retrieve()
                    .body(GithubUser.class);
            if (user == null || user.id() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unable to read GitHub profile"));
            }
            return ResponseEntity.ok(Map.of("valid", true, "user", user));
        } catch (RestClientException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "GitHub OAuth request failed"));
        }
    }

    @PostMapping("/github")
    public ResponseEntity<?> validateGithubToken(@Valid @RequestBody TokenRequest request) {
        try {
            GithubUser user = githubClient.get()
                    .uri("/user")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + request.accessToken())
                    .retrieve()
                    .body(GithubUser.class);

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

    public record TokenRequest(
            @NotBlank @Size(max = 4096) String accessToken) {}

    public record GithubToken(String access_token) {}

    public record GithubUser(
            Long id,
            String login,
            String name,
            String email,
            String avatar_url) {}
}

package com.hygienetracker.api.controller;

import com.hygienetracker.api.auth.AppTokenService;
import com.hygienetracker.api.auth.AuthUser;
import com.hygienetracker.api.error.Problem;
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

/**
 * {@code POST /api/auth/github}: finishes GitHub's Authorization Code + PKCE flow on the server
 * (the client secret never leaves the API) and returns the API's own JWT (Appendix I, 3.4).
 */
@RestController
@RequestMapping("/api/auth")
public class GithubAuthController {

    private static final String PATH = "/api/auth/github";

    private final RestClient githubClient;
    private final RestClient oauthClient;
    private final String clientId;
    private final String clientSecret;
    private final AppTokenService tokens;

    @Autowired
    public GithubAuthController(
            RestClient.Builder restClientBuilder,
            @Value("${github.oauth.client-id:}") String clientId,
            @Value("${github.oauth.client-secret:}") String clientSecret,
            AppTokenService tokens) {
        this(
                restClientBuilder.clone()
                        .baseUrl("https://api.github.com")
                        .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                        .defaultHeader(HttpHeaders.USER_AGENT, "hygiene-tracker-api")
                        .build(),
                restClientBuilder.clone()
                        .baseUrl("https://github.com")
                        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader(HttpHeaders.USER_AGENT, "hygiene-tracker-api")
                        .build(),
                clientId,
                clientSecret,
                tokens);
    }

    GithubAuthController(
            RestClient githubClient,
            RestClient oauthClient,
            String clientId,
            String clientSecret,
            AppTokenService tokens) {
        this.githubClient = githubClient;
        this.oauthClient = oauthClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokens = tokens;
    }

    /**
     * Exchanges the code with GitHub, reads the user's primary verified email, and signs in.
     * Returns 200 with a JWT, or 400 / 503 as a {@link Problem}.
     */
    @PostMapping("/github")
    public ResponseEntity<?> signInWithGithub(@Valid @RequestBody CodeRequest request) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return Problem.response(HttpStatus.SERVICE_UNAVAILABLE, "GitHub sign-in is not configured", PATH);
        }

        try {
            GithubToken token = exchangeCode(request);
            if (token == null || token.access_token() == null || token.access_token().isBlank()) {
                return Problem.response(
                        HttpStatus.BAD_REQUEST, "GitHub authorization code could not be exchanged", PATH);
            }

            GithubUser user = fetchGithubUser(token.access_token());
            if (user == null || user.id() == null) {
                return Problem.response(
                        HttpStatus.SERVICE_UNAVAILABLE, "GitHub profile could not be loaded", PATH);
            }
            if (!user.emailVerified() || user.email() == null || user.email().isBlank()) {
                return Problem.response(HttpStatus.BAD_REQUEST, "GitHub account email is not verified", PATH);
            }

            // TODO: replace with the stored user from find-or-create once users are saved.
            return ResponseEntity.ok(tokens.signIn(AuthUser.newUser(user.id(), user.email())));
        } catch (RestClientResponseException exception) {
            return Problem.response(
                    HttpStatus.BAD_REQUEST, "GitHub authorization code is invalid or expired", PATH);
        } catch (RestClientException exception) {
            return Problem.response(HttpStatus.SERVICE_UNAVAILABLE, "Unable to reach GitHub right now", PATH);
        }
    }

    private GithubToken exchangeCode(CodeRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", request.code());
        form.add("redirect_uri", request.redirectUri());
        form.add("code_verifier", request.codeVerifier());
        return oauthClient.post()
                .uri("/login/oauth/access_token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(GithubToken.class);
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
        String email = primaryVerifiedEmail(emails);
        return new GithubUser(
                profile.id(), profile.login(), profile.name(), email, email != null, profile.avatar_url());
    }

    /** Only the primary email, and only if GitHub has verified it (Appendix I, 2.2). */
    private static String primaryVerifiedEmail(GithubEmail[] emails) {
        if (emails == null) {
            return null;
        }
        for (GithubEmail email : emails) {
            if (email != null && email.primary() && email.verified()) {
                return email.email();
            }
        }
        return null;
    }

    /** GitHub's token response; only the access token is used. */
    public record GithubToken(String access_token) {}

    /** One entry from GitHub's {@code /user/emails}. */
    public record GithubEmail(String email, boolean primary, boolean verified) {}

    /** The client's sign-in request: the result of Authorization Code + PKCE. */
    public record CodeRequest(
            @NotBlank @Size(max = 4096) String code,
            @NotBlank @Size(max = 256) String codeVerifier,
            @NotBlank @Size(max = 512) String redirectUri) {}

    /** The fields used from GitHub's {@code /user}, plus the email chosen from {@code /user/emails}. */
    public record GithubUser(
            Long id,
            String login,
            String name,
            String email,
            boolean emailVerified,
            String avatar_url) {}
}

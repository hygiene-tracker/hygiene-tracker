package com.hygienetracker.api.controller;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.hygienetracker.api.auth.AppTokenService;
import com.hygienetracker.api.auth.AccountLinker;
import com.hygienetracker.api.auth.ProviderIdentity;
import com.hygienetracker.api.error.Problem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
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
    private final AccountLinker accounts;
    private final RestClient oauthClient;
    private final String webClientId;
    private final String webClientSecret;

    @Autowired
    public GoogleAuthController(
            @Value("${google.oauth.client-ids:}") String clientIds,
            @Value("${google.oauth.web-client-id:}") String webClientId,
            @Value("${google.oauth.web-client-secret:}") String webClientSecret,
            AppTokenService tokens,
            AccountLinker accounts,
            RestClient.Builder restClientBuilder) {
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
        this.accounts = accounts;
        this.oauthClient = restClientBuilder.clone()
                .baseUrl("https://oauth2.googleapis.com")
                .defaultHeader(org.springframework.http.HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.webClientId = webClientId;
        this.webClientSecret = webClientSecret;
    }

    GoogleAuthController(
            GoogleIdTokenVerifier verifier,
            AppTokenService tokens,
            AccountLinker accounts,
            RestClient oauthClient,
            String webClientId,
            String webClientSecret) {
        this.verifier = verifier;
        this.tokens = tokens;
        this.accounts = accounts;
        this.oauthClient = oauthClient;
        this.webClientId = webClientId;
        this.webClientSecret = webClientSecret;
    }

    /** Signs in with a Google ID token: 200 with a JWT, or 400 / 503 as a {@link Problem}. */
    @PostMapping("/google")
    public ResponseEntity<?> signInWithGoogle(@Valid @RequestBody TokenRequest request) {
        if ((request.idToken() == null || request.idToken().isBlank())
                && (request.code() == null || request.code().isBlank())) {
            return Problem.response(HttpStatus.BAD_REQUEST, "idToken or authorization code is required", PATH);
        }
        if (verifier == null) {
            return Problem.response(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not configured", PATH);
        }

        try {
            String idToken = request.idToken();
            if (idToken == null || idToken.isBlank()) {
                if (request.codeVerifier() == null || request.codeVerifier().isBlank()
                        || request.redirectUri() == null || request.redirectUri().isBlank()) {
                    return Problem.response(
                            HttpStatus.BAD_REQUEST,
                            "codeVerifier and redirectUri are required with an authorization code",
                            PATH);
                }
                idToken = exchangeCode(request);
            }
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                return Problem.response(HttpStatus.BAD_REQUEST, "Invalid Google ID token", PATH);
            }

            GoogleIdToken.Payload payload = token.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())
                    || payload.getEmail() == null || payload.getEmail().isBlank()) {
                return Problem.response(HttpStatus.BAD_REQUEST, "Google account email is not verified", PATH);
            }

            ProviderIdentity identity = new ProviderIdentity(
                    "GOOGLE", payload.getSubject(), payload.getEmail(), true);
            return ResponseEntity.ok(tokens.signIn(accounts.findOrCreate(identity)));
        } catch (AccountLinker.UnverifiedEmailException exception) {
            return Problem.response(HttpStatus.BAD_REQUEST, exception.getMessage(), PATH);
        } catch (AccountLinker.EmailConflictException exception) {
            return Problem.response(HttpStatus.CONFLICT, exception.getMessage(), PATH);
        } catch (RestClientException exception) {
            return Problem.response(HttpStatus.SERVICE_UNAVAILABLE, "Unable to reach Google right now", PATH);
        } catch (IllegalStateException exception) {
            return Problem.response(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not configured", PATH);
        } catch (GeneralSecurityException | IOException exception) {
            return Problem.response(
                    HttpStatus.SERVICE_UNAVAILABLE, "Unable to verify Google ID token right now", PATH);
        }
    }

    private String exchangeCode(TokenRequest request) {
        if (webClientId.isBlank() || webClientSecret.isBlank()) {
            throw new IllegalStateException("Google web OAuth is not configured");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", webClientId);
        form.add("client_secret", webClientSecret);
        form.add("code", request.code());
        form.add("code_verifier", request.codeVerifier());
        form.add("redirect_uri", request.redirectUri());
        form.add("grant_type", "authorization_code");
        GoogleTokenResponse response = oauthClient.post()
                .uri("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(GoogleTokenResponse.class);
        if (response == null || response.id_token() == null || response.id_token().isBlank()) {
            throw new RestClientException("Google did not return an ID token");
        }
        return response.id_token();
    }

    public record GoogleTokenResponse(String id_token) {}

    /** Supports the web code exchange and the native ID-token path from the appendix. */
    public record TokenRequest(
            @Size(max = 8192) String idToken,
            @Size(max = 4096) String code,
            @Size(max = 256) String codeVerifier,
            @Size(max = 512) String redirectUri) {}
}

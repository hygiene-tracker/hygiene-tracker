package com.hygienetracker.api.controller;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.hygienetracker.api.auth.AuthUser;
import com.hygienetracker.api.entity.User;
import com.hygienetracker.api.error.Problem;
import com.hygienetracker.api.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints for the currently authenticated user's profile. */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private static final String PATH = "/api/me";

    private final UserRepository users;

    public MeController(UserRepository users) {
        this.users = users;
    }

    /** Updates the signed-in user's display name using the email in the verified app JWT. */
    @PatchMapping
    @Transactional
    public ResponseEntity<?> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        String email = authenticatedEmail(authentication);
        if (email == null) {
            return Problem.response(HttpStatus.UNAUTHORIZED, "Authenticated user email is missing", PATH);
        }

        String displayName = request.displayName() == null ? null : request.displayName().trim();
        if (displayName == null || displayName.isEmpty()) {
            return Problem.response(HttpStatus.BAD_REQUEST, "Display name is required", PATH);
        }
        if (displayName.length() > 32) {
            return Problem.response(HttpStatus.BAD_REQUEST, "Display name must be 32 characters or fewer", PATH);
        }

        User user = users.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (user == null) {
            return Problem.response(HttpStatus.NOT_FOUND, "Authenticated user was not found", PATH);
        }

        user.setDisplayName(displayName);
        users.save(user);
        return ResponseEntity.ok(AuthUser.from(user));
    }

    private static String authenticatedEmail(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }
        String email = jwt.getClaimAsString("email");
        return email == null || email.isBlank() ? null : email;
    }

    public record UpdateProfileRequest(
            @JsonAlias("nickname") @Size(max = 32) String displayName) {}
}

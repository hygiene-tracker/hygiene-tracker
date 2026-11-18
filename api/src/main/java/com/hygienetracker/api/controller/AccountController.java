
package com.hygienetracker.api.controller;

import com.hygienetracker.api.error.Problem;
import com.hygienetracker.api.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private static final String PATH = "/api/account";

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAccount(
            @AuthenticationPrincipal Jwt jwt) {

        try {
            Long userId = Long.valueOf(jwt.getSubject());
            accountService.deleteAccount(userId);

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException exception) {
            return Problem.response(
                    HttpStatus.NOT_FOUND,
                    "User account not found",
                    PATH);
        }
    }
}

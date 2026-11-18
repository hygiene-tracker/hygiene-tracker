package com.hygienetracker.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.time.Instant;
import java.util.Optional;

import com.hygienetracker.api.auth.TestTokens;
import com.hygienetracker.api.entity.User;
import com.hygienetracker.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

class MeControllerTest {

    @Test
    void updatesDisplayNameForEmailFromAuthenticatedJwt() throws Exception {
        User user = user("student@example.com");
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMvc mockMvc = standaloneSetup(new MeController(users)).build();

        mockMvc.perform(patch("/api/me")
                        .contentType("application/json")
                        .content("{\"nickname\":\"Fresh Start\"}")
                        .principal(new JwtAuthenticationToken(jwt("student@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@example.com"));

        verify(users).findByEmail("student@example.com");
        verify(user).setDisplayName("Fresh Start");
        verify(users).save(user);
    }

    @Test
    void doesNotUpdateAUserWithADifferentEmail() throws Exception {
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmail("student@example.com")).thenReturn(Optional.empty());

        MockMvc mockMvc = standaloneSetup(new MeController(users)).build();

        mockMvc.perform(patch("/api/me")
                        .contentType("application/json")
                        .content("{\"displayName\":\"Fresh Start\"}")
                        .principal(new JwtAuthenticationToken(jwt("student@example.com"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsBlankDisplayName() throws Exception {
        UserRepository users = mock(UserRepository.class);
        MockMvc mockMvc = standaloneSetup(new MeController(users)).build();

        mockMvc.perform(patch("/api/me")
                        .contentType("application/json")
                        .content("{\"displayName\":\"   \"}")
                        .principal(new JwtAuthenticationToken(jwt("student@example.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Display name is required"));
    }

    private static User user(String email) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(user.getEmail()).thenReturn(email);
        when(user.getRole()).thenReturn("USER");
        return user;
    }

    private static org.springframework.security.oauth2.jwt.Jwt jwt(String email) {
        return org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .claim("sub", "7")
                .claim("email", email)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .issuer("hygiene-tracker-api")
                .build();
    }
}

package com.hygienetracker.api.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class GoogleAuthControllerTest {

    @Test
    void returnsContractAuthResponseForVerifiedGoogleIdToken() throws Exception {
        GoogleIdTokenVerifier verifier = mock(GoogleIdTokenVerifier.class);
        GoogleIdToken googleToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);

        when(verifier.verify("mock-google-id-token")).thenReturn(googleToken);
        when(googleToken.getPayload()).thenReturn(payload);
        when(payload.getSubject()).thenReturn("google-subject-123");
        when(payload.getEmail()).thenReturn("student@example.com");
        when(payload.getEmailVerified()).thenReturn(true);

        MockMvc mockMvc = standaloneSetup(
                new GoogleAuthController(verifier, "test-secret-that-is-at-least-32-bytes-long"))
                .build();

        mockMvc.perform(post("/api/auth/google")
                        .contentType("application/json")
                        .content("{\"idToken\":\"mock-google-id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.email").value("student@example.com"))
                .andExpect(jsonPath("$.user.role").value("USER"));
    }

    @Test
    void rejectsGoogleIdTokenForUnverifiedEmail() throws Exception {
        GoogleIdTokenVerifier verifier = mock(GoogleIdTokenVerifier.class);
        GoogleIdToken googleToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);

        when(verifier.verify("mock-unverified-google-id-token")).thenReturn(googleToken);
        when(googleToken.getPayload()).thenReturn(payload);
        when(payload.getEmailVerified()).thenReturn(false);

        MockMvc mockMvc = standaloneSetup(
                new GoogleAuthController(verifier, "test-secret-that-is-at-least-32-bytes-long"))
                .build();

        mockMvc.perform(post("/api/auth/google")
                        .contentType("application/json")
                        .content("{\"idToken\":\"mock-unverified-google-id-token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Google account email is not verified"));
    }
}

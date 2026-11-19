package com.hygienetracker.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.List;
import java.util.Optional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.hygienetracker.api.auth.AccountLinker;
import com.hygienetracker.api.auth.TestTokens;
import com.hygienetracker.api.entity.Identity;
import com.hygienetracker.api.entity.User;
import com.hygienetracker.api.repository.IdentityRepository;
import com.hygienetracker.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

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
                new GoogleAuthController(verifier, TestTokens.service(), storedAccountsForNewUser(), null, "", ""))
                .build();

        mockMvc.perform(post("/api/auth/google")
                        .contentType("application/json")
                        .content("{\"idToken\":\"mock-google-id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.id").value(123L))
                .andExpect(jsonPath("$.user.email").value("student@example.com"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.displayName").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.user").value(org.hamcrest.Matchers.hasKey("displayName")));
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
                new GoogleAuthController(verifier, TestTokens.service(), storedAccountsForNewUser(), null, "", ""))
                .build();

        mockMvc.perform(post("/api/auth/google")
                        .contentType("application/json")
                        .content("{\"idToken\":\"mock-unverified-google-id-token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Google account email is not verified"));
    }

    @Test
    void returnsConflictWhenGoogleEmailBelongsToGithubUser() throws Exception {
        GoogleIdTokenVerifier verifier = mock(GoogleIdTokenVerifier.class);
        GoogleIdToken googleToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);

        when(verifier.verify("mock-conflict-google-id-token")).thenReturn(googleToken);
        when(googleToken.getPayload()).thenReturn(payload);
        when(payload.getSubject()).thenReturn("google-subject-456");
        when(payload.getEmail()).thenReturn("student@example.com");
        when(payload.getEmailVerified()).thenReturn(true);

        MockMvc mockMvc = standaloneSetup(
                new GoogleAuthController(
                        verifier, TestTokens.service(), storedAccountsForExistingGithubUser(), null, "", ""))
                .build();

        mockMvc.perform(post("/api/auth/google")
                        .contentType("application/json")
                        .content("{\"idToken\":\"mock-conflict-google-id-token\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("This email is registered with GitHub"));
    }

    private static AccountLinker storedAccountsForNewUser() {
        UserRepository users = mock(UserRepository.class);
        IdentityRepository identities = mock(IdentityRepository.class);
        User storedUser = storedUser(123L, "student@example.com");
        when(identities.findByProviderAndProviderUserId("GOOGLE", "google-subject-123"))
                .thenReturn(Optional.empty());
        when(users.findByEmail("student@example.com")).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenReturn(storedUser);
        return new AccountLinker(users, identities);
    }

    private static AccountLinker storedAccountsForExistingGithubUser() {
        UserRepository users = mock(UserRepository.class);
        IdentityRepository identities = mock(IdentityRepository.class);
        User storedUser = storedUser(456L, "student@example.com");
        Identity storedIdentity = storedIdentity(storedUser, "GITHUB", "github-subject-123");
        when(identities.findByProviderAndProviderUserId("GOOGLE", "google-subject-456"))
                .thenReturn(Optional.empty());
        when(users.findByEmail("student@example.com")).thenReturn(Optional.of(storedUser));
        when(identities.findByUser_Id(456L)).thenReturn(List.of(storedIdentity));
        return new AccountLinker(users, identities);
    }

    private static User storedUser(long id, String email) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getEmail()).thenReturn(email);
        when(user.getRole()).thenReturn("USER");
        return user;
    }

    private static Identity storedIdentity(User user, String provider, String providerUserId) {
        Identity identity = mock(Identity.class);
        when(identity.getUser()).thenReturn(user);
        when(identity.getProvider()).thenReturn(provider);
        when(identity.getProviderUserId()).thenReturn(providerUserId);
        return identity;
    }
}

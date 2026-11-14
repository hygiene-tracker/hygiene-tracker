package com.hygienetracker.api.controller;

import com.hygienetracker.api.auth.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class GithubAuthControllerTest {

    @Test
    void returnsApplicationTokenForVerifiedGithubEmail() throws Exception {
        TestClients clients = testClients(true, true);

        MockMvc mockMvc = standaloneSetup(controller(clients.githubClient(), clients.oauthClient())).build();

        mockMvc.perform(post("/api/auth/github")
                        .contentType("application/json")
                        .content("{\"code\":\"github-code\",\"codeVerifier\":\"github-verifier\",\"redirectUri\":\"hygiene-tracker://auth/github\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.id").value(12345))
                .andExpect(jsonPath("$.user.email").value("student@example.com"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.displayName").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void rejectsGithubAccountWithoutVerifiedEmail() throws Exception {
        TestClients clients = testClients(true, false);

        MockMvc mockMvc = standaloneSetup(controller(clients.githubClient(), clients.oauthClient())).build();

        mockMvc.perform(post("/api/auth/github")
                        .contentType("application/json")
                        .content("{\"code\":\"github-code\",\"codeVerifier\":\"github-verifier\",\"redirectUri\":\"hygiene-tracker://auth/github\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("GitHub account email is not verified"));
    }

    @Test
    void rejectsVerifiedEmailThatIsNotPrimary() throws Exception {
        TestClients clients = testClients(false, true);

        MockMvc mockMvc = standaloneSetup(controller(clients.githubClient(), clients.oauthClient())).build();

        mockMvc.perform(post("/api/auth/github")
                        .contentType("application/json")
                        .content("{\"code\":\"github-code\",\"codeVerifier\":\"github-verifier\",\"redirectUri\":\"hygiene-tracker://auth/github\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("GitHub account email is not verified"));
    }

    private static GithubAuthController controller(RestClient githubClient, RestClient oauthClient) {
        return new GithubAuthController(
                githubClient, oauthClient, "github-client-id", "github-client-secret", TestTokens.service());
    }

    private static TestClients testClients(boolean primaryEmail, boolean verifiedEmail) {
        RestClient githubClient = mock(RestClient.class);
        RestClient oauthClient = mock(RestClient.class);

        RestClient.RequestBodyUriSpec tokenRequest = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.ResponseSpec tokenResponse = mock(RestClient.ResponseSpec.class);
        when(oauthClient.post()).thenReturn(tokenRequest);
        when(tokenRequest.uri("/login/oauth/access_token")).thenReturn(tokenRequest);
        when(tokenRequest.contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED))
                .thenReturn(tokenRequest);
        when(tokenRequest.body(org.mockito.ArgumentMatchers.any(Object.class))).thenReturn(tokenRequest);
        when(tokenRequest.retrieve()).thenReturn(tokenResponse);
        when(tokenResponse.body(GithubAuthController.GithubToken.class))
                .thenReturn(new GithubAuthController.GithubToken("github-access-token"));

        RestClient.RequestHeadersUriSpec userRequest = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec userHeaders = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec userResponse = mock(RestClient.ResponseSpec.class);
        when(githubClient.get()).thenReturn(userRequest);
        when(userRequest.uri("/user")).thenReturn(userHeaders);
        when(userHeaders.header("Authorization", "Bearer github-access-token")).thenReturn(userHeaders);
        when(userHeaders.retrieve()).thenReturn(userResponse);
        when(userResponse.body(GithubAuthController.GithubProfile.class))
                .thenReturn(new GithubAuthController.GithubProfile(
                        12345L, "student", "Student", "https://github.com/avatar.png"));

        RestClient.RequestHeadersUriSpec emailRequest = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec emailHeaders = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec emailResponse = mock(RestClient.ResponseSpec.class);
        when(githubClient.get()).thenReturn(userRequest, emailRequest);
        when(emailRequest.uri("/user/emails?per_page=100")).thenReturn(emailHeaders);
        when(emailHeaders.header("Authorization", "Bearer github-access-token")).thenReturn(emailHeaders);
        when(emailHeaders.retrieve()).thenReturn(emailResponse);
        when(emailResponse.body(GithubAuthController.GithubEmail[].class))
                .thenReturn(new GithubAuthController.GithubEmail[]{
                        new GithubAuthController.GithubEmail("student@example.com", primaryEmail, verifiedEmail)});

        return new TestClients(githubClient, oauthClient);
    }

    private record TestClients(
            RestClient githubClient,
            RestClient oauthClient) {}
}

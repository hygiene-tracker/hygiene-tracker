package com.hygienetracker.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.hygienetracker.api.entity.Identity;
import com.hygienetracker.api.entity.User;
import com.hygienetracker.api.repository.IdentityRepository;
import com.hygienetracker.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountLinkerTest {

    private UserRepository users;
    private IdentityRepository identities;
    private AccountLinker linker;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        identities = mock(IdentityRepository.class);
        linker = new AccountLinker(users, identities);
    }

    @Test
    void existingProviderIdentityReturnsTheSameUser() {
        User user = user(7L, "student@example.com", "USER");
        Identity identity = identity(user, "GOOGLE", "google-sub");
        when(identities.findByProviderAndProviderUserId("GOOGLE", "google-sub"))
                .thenReturn(Optional.of(identity));

        AuthUser result = linker.findOrCreate(new ProviderIdentity("google", "google-sub", "student@example.com", true));

        assertThat(result.id()).isEqualTo(7L);
        verify(users, never()).findByEmail(any());
    }

    @Test
    void verifiedEmailAlreadyUsedByOtherProviderReturnsConflictWithoutSaving() {
        User user = user(7L, "student@example.com", "USER");
        when(users.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        Identity existingIdentity = identity(user, "GITHUB", "123");
        when(identities.findByUser_Id(7L)).thenReturn(List.of(existingIdentity));

        assertThatThrownBy(() -> linker.findOrCreate(
                new ProviderIdentity("GOOGLE", "google-sub", "student@example.com", true)))
                .isInstanceOf(AccountLinker.EmailConflictException.class)
                .hasMessage("This email is registered with GitHub");

        verify(identities, never()).save(any());
    }

    @Test
    void unverifiedEmailIsRejectedBeforeAnyDatabaseWrite() {
        when(identities.findByProviderAndProviderUserId("GITHUB", "123"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> linker.findOrCreate(
                new ProviderIdentity("GITHUB", "123", "student@example.com", false)))
                .isInstanceOf(AccountLinker.UnverifiedEmailException.class);

        verify(identities).findByProviderAndProviderUserId("GITHUB", "123");
        verifyNoInteractions(users);
    }

    @Test
    void newVerifiedIdentityCreatesUserAndIdentity() {
        User saved = user(8L, "student@example.com", "USER");
        when(users.findByEmail("student@example.com")).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenReturn(saved);

        AuthUser result = linker.findOrCreate(
                new ProviderIdentity("GITHUB", "123", "STUDENT@example.com", true));

        assertThat(result.id()).isEqualTo(8L);
        verify(users).save(any(User.class));
        verify(identities).save(any(Identity.class));
    }

    private static User user(long id, String email, String role) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getEmail()).thenReturn(email);
        when(user.getRole()).thenReturn(role);
        return user;
    }

    private static Identity identity(User user, String provider, String providerUserId) {
        Identity identity = mock(Identity.class);
        when(identity.getUser()).thenReturn(user);
        when(identity.getProvider()).thenReturn(provider);
        when(identity.getProviderUserId()).thenReturn(providerUserId);
        return identity;
    }
}

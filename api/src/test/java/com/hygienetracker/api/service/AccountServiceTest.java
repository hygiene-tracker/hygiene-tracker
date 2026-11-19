
package com.hygienetracker.api.service;

import java.util.List;
import java.util.Optional;

import com.hygienetracker.api.entity.Identity;
import com.hygienetracker.api.repository.IdentityRepository;
import com.hygienetracker.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    private UserRepository users;
    private IdentityRepository identities;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        identities = mock(IdentityRepository.class);
        accountService = new AccountService(users, identities);
    }

    @Test
    void deletesIdentitiesBeforeDeletingUser() {
        Long userId = 1L;
        Identity identity = new Identity();

        when(users.existsById(userId)).thenReturn(true);
        when(identities.findByUser_Id(userId)).thenReturn(List.of(identity));

        accountService.deleteAccount(userId);

        verify(identities).deleteAll(List.of(identity));
        verify(users).deleteById(userId);
    }

    @Test
    void deletesUserWhenNoIdentitiesExist() {
        Long userId = 1L;

        when(users.existsById(userId)).thenReturn(true);
        when(identities.findByUser_Id(userId)).thenReturn(List.of());

        accountService.deleteAccount(userId);

        verify(identities).deleteAll(List.of());
        verify(users).deleteById(userId);
    }

    @Test
    void rejectsDeletionWhenUserDoesNotExist() {
        Long userId = 999L;

        when(users.existsById(userId)).thenReturn(false);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> accountService.deleteAccount(userId))
                .withMessage("User account not found");

        verifyNoInteractions(identities);
        verify(users, never()).deleteById(userId);
    }
}

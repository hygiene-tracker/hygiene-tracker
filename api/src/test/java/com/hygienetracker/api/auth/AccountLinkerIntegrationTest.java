package com.hygienetracker.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.hygienetracker.api.entity.Identity;
import com.hygienetracker.api.repository.IdentityRepository;
import com.hygienetracker.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** Exercises find-or-create against the same PostgreSQL/Testcontainers schema used by the API. */
@SpringBootTest
@Transactional
class AccountLinkerIntegrationTest {

    @Autowired
    private AccountLinker linker;

    @Autowired
    private UserRepository users;

    @Autowired
    private IdentityRepository identities;

    @Test
    void signingInTwiceWithTheSameIdentityLeavesOneUserAndOneIdentity() {
        long usersBefore = users.count();
        long identitiesBefore = identities.count();
        ProviderIdentity providerIdentity = new ProviderIdentity(
                "GOOGLE", "google-repeat-subject", "repeat@example.com", true);

        AuthUser first = linker.findOrCreate(providerIdentity);
        AuthUser second = linker.findOrCreate(providerIdentity);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(users.count()).isEqualTo(usersBefore + 1);
        assertThat(identities.count()).isEqualTo(identitiesBefore + 1);

        Identity storedIdentity = identities
                .findByProviderAndProviderUserId("GOOGLE", "google-repeat-subject")
                .orElseThrow();
        assertThat(storedIdentity.getUser().getId()).isEqualTo(first.id());
        assertThat(users.findByEmail("repeat@example.com")).isPresent();
    }
}

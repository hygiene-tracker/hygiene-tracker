package com.hygienetracker.api.auth;

import java.util.Locale;

import com.hygienetracker.api.entity.Identity;
import com.hygienetracker.api.entity.User;
import com.hygienetracker.api.repository.IdentityRepository;
import com.hygienetracker.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies Appendix I's identity lookup and verified-email account rule. */
@Service
public class AccountLinker {

    private final UserRepository users;
    private final IdentityRepository identities;

    public AccountLinker(UserRepository users, IdentityRepository identities) {
        this.users = users;
        this.identities = identities;
    }

    @Transactional
    public AuthUser findOrCreate(ProviderIdentity identity) {
        String provider = identity.provider().trim().toUpperCase(Locale.ROOT);
        String providerUserId = require(identity.providerUserId(), "provider user ID");
        Identity existingIdentity = identities
                .findByProviderAndProviderUserId(provider, providerUserId)
                .orElse(null);
        if (existingIdentity != null) {
            return AuthUser.from(existingIdentity.getUser());
        }

        if (!identity.emailVerified()) {
            throw new UnverifiedEmailException(provider);
        }
        String email = require(identity.email(), "email").trim().toLowerCase(Locale.ROOT);

        User existingUser = users.findByEmail(email).orElse(null);
        if (existingUser != null) {
            String existingProvider = identities.findByUser_Id(existingUser.getId()).stream()
                    .findFirst()
                    .map(Identity::getProvider)
                    .orElse("another provider");
            throw new EmailConflictException(existingProvider);
        }

        User user = new User();
        user.setEmail(email);
        user = users.save(user);

        Identity newIdentity = new Identity();
        newIdentity.setUser(user);
        newIdentity.setProvider(provider);
        newIdentity.setProviderUserId(providerUserId);
        identities.save(newIdentity);

        return AuthUser.from(user);
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Provider returned no " + field);
        }
        return value;
    }

    public static final class UnverifiedEmailException extends RuntimeException {
        public UnverifiedEmailException(String provider) {
            super("Verify your email with " + titleCase(provider) + ", then sign in again");
        }
    }

    public static final class EmailConflictException extends RuntimeException {
        private final String provider;

        public EmailConflictException(String provider) {
            super("This email is registered with " + titleCase(provider));
            this.provider = provider;
        }

        public String provider() {
            return provider;
        }
    }

    private static String titleCase(String provider) {
        if (provider == null || provider.isBlank()) {
            return "another provider";
        }
        String normalized = provider.toLowerCase(Locale.ROOT);
        if ("github".equals(normalized)) return "GitHub";
        if ("google".equals(normalized)) return "Google";
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }
}


package com.hygienetracker.api.service;

import com.hygienetracker.api.repository.IdentityRepository;
import com.hygienetracker.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final UserRepository users;
    private final IdentityRepository identities;

    public AccountService(
            UserRepository users,
            IdentityRepository identities) {
        this.users = users;
        this.identities = identities;
    }

    @Transactional
    public void deleteAccount(Long userId) {
        if (!users.existsById(userId)) {
            throw new IllegalArgumentException("User account not found");
        }

        identities.deleteAll(identities.findByUser_Id(userId));
        users.deleteById(userId);
    }
}

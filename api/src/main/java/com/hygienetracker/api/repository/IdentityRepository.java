package com.hygienetracker.api.repository;

import java.util.List;
import java.util.Optional;

import com.hygienetracker.api.entity.Identity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityRepository extends JpaRepository<Identity, Long> {
    Optional<Identity> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<Identity> findByUser_Id(Long userId);
}

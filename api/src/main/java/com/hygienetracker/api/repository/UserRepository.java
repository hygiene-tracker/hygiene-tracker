package com.hygienetracker.api.repository;

import java.util.Optional;

import com.hygienetracker.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}

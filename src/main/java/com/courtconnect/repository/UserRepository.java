package com.courtconnect.repository;

import com.courtconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<com.courtconnect.model.User, Long> {
    Optional<User> findByUsername(String username);
}
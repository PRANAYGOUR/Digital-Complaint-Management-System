package com.dcs.repository;

import com.dcs.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    // Allow login via email as well
    Optional<User> findByEmail(String email);
    // Case-insensitive lookups to make login robust
    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByEmailIgnoreCase(String email);
}

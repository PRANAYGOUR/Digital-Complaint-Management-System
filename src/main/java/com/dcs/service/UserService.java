package com.dcs.service;

import com.dcs.model.User;
import com.dcs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User register(User user) {
        return userRepository.save(user);
    }

    public Optional<User> login(String identifier, String password) {
        // Normalize identifier only; password must match exactly (case-sensitive, no trimming)
        String id = identifier == null ? "" : identifier.trim();
        String pwd = password == null ? "" : password; // do NOT trim password
        if (id.isEmpty() || pwd.isEmpty()) return Optional.empty();

        // Try lookup by username/email, case-insensitive for robustness
        Optional<User> user = userRepository.findByUsername(id)
                .or(() -> userRepository.findByEmail(id))
                .or(() -> userRepository.findByUsernameIgnoreCase(id))
                .or(() -> userRepository.findByEmailIgnoreCase(id));
        if (user.isPresent() && user.get().getPassword() != null && user.get().getPassword().equals(pwd)) {
            return user;
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}

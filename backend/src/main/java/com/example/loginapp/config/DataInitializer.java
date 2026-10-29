package com.example.loginapp.config;

import com.example.loginapp.entity.User;
import com.example.loginapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Seeds demo users into the in-memory database. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createUser("admin", "Admin@123", "Administrator", "admin@example.com", "ADMIN");
        createUser("john", "John@123", "John Doe", "john.doe@example.com", "USER");
        log.info("Seeded demo users: admin / Admin@123, john / John@123");
    }

    private void createUser(String username, String password, String fullName, String email, String role) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role);
        userRepository.save(user);
    }
}

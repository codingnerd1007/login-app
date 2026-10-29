package com.example.loginapp.controller;

import com.example.loginapp.dto.UserProfileDto;
import com.example.loginapp.exception.InvalidCredentialsException;
import com.example.loginapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> me(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .map(u -> new UserProfileDto(u.getUsername(), u.getFullName(), u.getEmail(), u.getRole(), u.getLastLoginAt()))
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
    }
}

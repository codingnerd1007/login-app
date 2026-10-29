package com.example.loginapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserProfileDto {

    private String username;
    private String fullName;
    private String email;
    private String role;
    private LocalDateTime lastLoginAt;
}

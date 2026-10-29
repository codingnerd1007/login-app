package com.example.loginapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponseDto {

    private String token;
    private String tokenType;
    private long expiresInSeconds;
    private String username;
    private String fullName;
    private String role;
}

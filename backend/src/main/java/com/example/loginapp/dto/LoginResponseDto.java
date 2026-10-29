package com.example.loginapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDto {

    private boolean otpRequired;
    private String maskedEmail;
    private long otpExpirySeconds;
    private String message;
}

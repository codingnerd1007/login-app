package com.example.loginapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ErrorResponseDto {

    private LocalDateTime timestamp;
    private int status;
    private String errorCode;
    private String message;
}

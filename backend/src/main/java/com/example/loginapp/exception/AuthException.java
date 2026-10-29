package com.example.loginapp.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Base exception for the login flow, mapped to an HTTP response by GlobalExceptionHandler. */
@Getter
public class AuthException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public AuthException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}

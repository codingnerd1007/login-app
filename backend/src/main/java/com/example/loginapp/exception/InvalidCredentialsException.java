package com.example.loginapp.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends AuthException {

    public InvalidCredentialsException(String message) {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", message);
    }
}

package com.example.loginapp.exception;

import org.springframework.http.HttpStatus;

public class NonceUnavailableException extends AuthException {

    public NonceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "NONCE_UNAVAILABLE", message);
    }
}

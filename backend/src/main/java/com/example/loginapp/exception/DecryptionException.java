package com.example.loginapp.exception;

import org.springframework.http.HttpStatus;

public class DecryptionException extends AuthException {

    public DecryptionException(String message) {
        super(HttpStatus.BAD_REQUEST, "DECRYPTION_FAILED", message);
    }
}

package com.example.loginapp.exception;

import org.springframework.http.HttpStatus;

public class ReplayDetectedException extends AuthException {

    public ReplayDetectedException(String message) {
        super(HttpStatus.BAD_REQUEST, "REPLAY_DETECTED", message);
    }
}

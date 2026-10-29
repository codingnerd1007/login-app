package com.example.loginapp.exception;

import org.springframework.http.HttpStatus;

public class InvalidCaptchaException extends AuthException {

    public InvalidCaptchaException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_CAPTCHA", message);
    }
}

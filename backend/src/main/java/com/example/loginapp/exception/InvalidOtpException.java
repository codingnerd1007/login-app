package com.example.loginapp.exception;

import org.springframework.http.HttpStatus;

public class InvalidOtpException extends AuthException {

    public InvalidOtpException(String message) {
        super(HttpStatus.UNAUTHORIZED, "INVALID_OTP", message);
    }

    /** Use for OTP_EXPIRED: the OTP can no longer be used and the user must login again. */
    public InvalidOtpException(String errorCode, String message) {
        super(HttpStatus.UNAUTHORIZED, errorCode, message);
    }
}

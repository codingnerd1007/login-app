package com.example.loginapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mock email sender. The OTP is written to the application log instead of being emailed.
 * Replace with JavaMailSender (or the office mail service) when wiring a real mail server.
 */
@Slf4j
@Service
public class EmailService {

    public void sendOtpEmail(String toEmail, String otp, long expirySeconds) {
        log.info("[MOCK EMAIL] To: {} | Your login OTP is {} (valid for {} minutes)",
                toEmail, otp, expirySeconds / 60);
    }
}

package com.example.loginapp.service;

import com.example.loginapp.exception.InvalidOtpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates and validates login OTPs. One active OTP per username; a new login replaces the old one.
 */
@Service
public class OtpService {

    private final SecureRandom random = new SecureRandom();
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    @Value("${app.otp.length}")
    private int otpLength;

    @Value("${app.otp.expiry-seconds}")
    private long expirySeconds;

    @Value("${app.otp.max-attempts}")
    private int maxAttempts;

    public String generateOtp(String username) {
        StringBuilder sb = new StringBuilder(otpLength);
        for (int i = 0; i < otpLength; i++) {
            sb.append(random.nextInt(10));
        }
        String otp = sb.toString();
        otpStore.put(username, new OtpEntry(otp, Instant.now().plusSeconds(expirySeconds)));
        return otp;
    }

    public void validateOtp(String username, String otp) {
        OtpEntry entry = otpStore.get(username);
        if (entry == null) {
            throw new InvalidOtpException("OTP_EXPIRED", "OTP not found. Please login again.");
        }
        if (entry.expiresAt.isBefore(Instant.now())) {
            otpStore.remove(username);
            throw new InvalidOtpException("OTP_EXPIRED", "OTP has expired. Please login again.");
        }
        if (otp == null || !entry.otp.equals(otp.trim())) {
            entry.attempts++;
            if (entry.attempts >= maxAttempts) {
                otpStore.remove(username);
                throw new InvalidOtpException("OTP_EXPIRED", "Maximum OTP attempts exceeded. Please login again.");
            }
            throw new InvalidOtpException("Invalid OTP. " + (maxAttempts - entry.attempts) + " attempt(s) left.");
        }
        otpStore.remove(username);
    }

    public long getExpirySeconds() {
        return expirySeconds;
    }

    private static class OtpEntry {
        private final String otp;
        private final Instant expiresAt;
        private int attempts;

        OtpEntry(String otp, Instant expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
        }
    }
}

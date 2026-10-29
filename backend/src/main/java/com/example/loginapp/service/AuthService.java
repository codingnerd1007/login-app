package com.example.loginapp.service;

import com.example.loginapp.dto.AuthResponseDto;
import com.example.loginapp.dto.LoginRequestDto;
import com.example.loginapp.dto.LoginResponseDto;
import com.example.loginapp.dto.OtpVerifyRequestDto;
import com.example.loginapp.entity.User;
import com.example.loginapp.exception.InvalidCredentialsException;
import com.example.loginapp.repository.UserRepository;
import com.example.loginapp.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final JwtService jwtService;

    /** Step 1: captcha, then username/password. On success an OTP is generated and emailed. */
    public LoginResponseDto login(LoginRequestDto request) {
        captchaService.validateCaptcha(request.getCaptchaId(), request.getCaptchaText());

        User user = userRepository.findByUsername(request.getUsername())
                .filter(User::isActive)
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS));

        // getPassword() returns the decrypted password (see LoginRequestDto)
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS);
        }

        String otp = otpService.generateOtp(user.getUsername());
        emailService.sendOtpEmail(user.getEmail(), otp, otpService.getExpirySeconds());
        log.info("Credentials verified for user {}, OTP sent", user.getUsername());

        return new LoginResponseDto(true, maskEmail(user.getEmail()), otpService.getExpirySeconds(),
                "OTP has been sent to your registered email");
    }

    /** Step 2: OTP. On success a JWT is issued. */
    @Transactional
    public AuthResponseDto verifyOtp(OtpVerifyRequestDto request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS));

        // getOtp() returns the decrypted OTP (see OtpVerifyRequestDto)
        otpService.validateOtp(user.getUsername(), request.getOtp());

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("User {} logged in", user.getUsername());

        return new AuthResponseDto(jwtService.generateToken(user), "Bearer", jwtService.getExpirySeconds(),
                user.getUsername(), user.getFullName(), user.getRole());
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 2) {
            return email;
        }
        return email.substring(0, 2) + "*".repeat(at - 2) + email.substring(at);
    }
}

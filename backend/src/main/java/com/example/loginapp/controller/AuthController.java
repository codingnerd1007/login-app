package com.example.loginapp.controller;

import com.example.loginapp.dto.AuthResponseDto;
import com.example.loginapp.dto.CaptchaResponseDto;
import com.example.loginapp.dto.LoginRequestDto;
import com.example.loginapp.dto.LoginResponseDto;
import com.example.loginapp.dto.OtpVerifyRequestDto;
import com.example.loginapp.dto.PublicKeyResponseDto;
import com.example.loginapp.service.AuthService;
import com.example.loginapp.service.CaptchaService;
import com.example.loginapp.util.RSAUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CaptchaService captchaService;
    private final AuthService authService;

    @GetMapping("/captcha")
    public ResponseEntity<CaptchaResponseDto> getCaptcha() {
        return ResponseEntity.ok(captchaService.generateCaptcha());
    }

    @GetMapping("/public-key")
    public ResponseEntity<PublicKeyResponseDto> getPublicKey() {
        return ResponseEntity.ok(new PublicKeyResponseDto(RSAUtil.getPublicKeyBase64()));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponseDto> verifyOtp(@Valid @RequestBody OtpVerifyRequestDto request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }
}

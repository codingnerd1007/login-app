package com.example.loginapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CaptchaResponseDto {

    private String captchaId;

    /** data:image/png;base64,... */
    private String captchaImage;
}

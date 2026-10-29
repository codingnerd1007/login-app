package com.example.loginapp.dto;

import com.example.loginapp.util.RSAUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "password")
public class LoginRequestDto {

    @NotBlank(message = "Username is required")
    private String username;

    /** RSA-OAEP encrypted (Base64) password as sent by the frontend. */
    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Captcha id is required")
    private String captchaId;

    @NotBlank(message = "Captcha is required")
    private String captchaText;

    private String browser;
    private String os;

    /** Returns the decrypted (plain text) password. */
    public String getPassword() {
        return RSAUtil.decrypt(password);
    }
}

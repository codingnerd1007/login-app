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
@ToString(exclude = "otp")
public class OtpVerifyRequestDto {

    @NotBlank(message = "Username is required")
    private String username;

    /** RSA-OAEP encrypted (Base64) OTP as sent by the frontend. */
    @NotBlank(message = "OTP is required")
    private String otp;

    /** Returns the decrypted (plain text) OTP. */
    public String getOtp() {
        return RSAUtil.decrypt(otp);
    }
}

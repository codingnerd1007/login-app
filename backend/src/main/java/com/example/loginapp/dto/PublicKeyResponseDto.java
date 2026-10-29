package com.example.loginapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PublicKeyResponseDto {

    /** RSA public key, X.509 SPKI, Base64 encoded. */
    private String publicKey;
}

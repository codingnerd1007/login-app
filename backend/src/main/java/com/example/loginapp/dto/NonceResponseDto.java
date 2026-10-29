package com.example.loginapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NonceResponseDto {

    /** Single-use value to put inside the next encrypted field ("nonce:value"). */
    private String nonce;
}

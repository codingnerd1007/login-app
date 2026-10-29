package com.example.loginapp.util;

import com.example.loginapp.exception.DecryptionException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Symmetric AES encryption shared with the Angular frontend (CryptoJS).
 * The key and IV must match environment.ts on the frontend.
 * AES/CBC/PKCS5Padding in Java == CryptoJS AES with mode CBC and padding Pkcs7.
 */
public final class AESUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY = "LoginApp@AESKey1";
    private static final String INIT_VECTOR = "LoginApp@AESIv01";

    private AESUtil() {
    }

    public static String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            Cipher cipher = initCipher(Cipher.ENCRYPT_MODE);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public static String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            Cipher cipher = initCipher(Cipher.DECRYPT_MODE);
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new DecryptionException("Unable to decrypt request payload");
        }
    }

    private static Cipher initCipher(int mode) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(INIT_VECTOR.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(mode, keySpec, ivSpec);
        return cipher;
    }
}

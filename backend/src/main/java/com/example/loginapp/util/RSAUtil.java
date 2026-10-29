package com.example.loginapp.util;

import com.example.loginapp.exception.DecryptionException;
import com.example.loginapp.exception.ReplayDetectedException;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import java.util.function.Consumer;

/**
 * RSA-OAEP (SHA-256) decryption of values encrypted by the Angular frontend with Web Crypto.
 * Only the public key is given to the frontend; the private key stays on the server and is
 * loaded from the PKCS12 keystore at startup by RsaKeyStoreConfig.
 *
 * Every encrypted value carries a single-use nonce from GET /api/auth/nonce as "nonce:value",
 * so a captured ciphertext cannot be replayed.
 */
public final class RSAUtil {

    private static final String ALGORITHM = "RSA/ECB/OAEPPadding";

    // Explicit SHA-256 for both the OAEP digest and MGF1. Java's "OAEPWithSHA-256AndMGF1Padding"
    // defaults MGF1 to SHA-1, which does not match Web Crypto's RSA-OAEP with SHA-256.
    private static final OAEPParameterSpec OAEP_SHA256 = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

    private static volatile PrivateKey privateKey;
    private static volatile PublicKey publicKey;
    private static volatile Consumer<String> nonceValidator;

    private RSAUtil() {
    }

    public static void init(PrivateKey privateKey, PublicKey publicKey) {
        RSAUtil.privateKey = privateKey;
        RSAUtil.publicKey = publicKey;
    }

    /** Registered by NonceService; throws if the nonce is unknown, expired or already used. */
    public static void setNonceValidator(Consumer<String> nonceValidator) {
        RSAUtil.nonceValidator = nonceValidator;
    }

    /** Public key in X.509 SubjectPublicKeyInfo (SPKI) form, Base64 encoded, for the frontend. */
    public static String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(requireInitialized(publicKey).getEncoded());
    }

    /**
     * Decrypts "nonce:value", consumes the nonce and returns the value.
     * Because the nonce is consumed, each encrypted value can be decrypted only once:
     * call the DTO getters (getPassword(), getOtp()) once per request and keep the result.
     */
    public static String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        String payload = decryptRaw(cipherText);
        int separator = payload.indexOf(':');
        if (separator <= 0) {
            throw new ReplayDetectedException("Request is missing its nonce. Please try again.");
        }
        requireInitialized(nonceValidator).accept(payload.substring(0, separator));
        return payload.substring(separator + 1);
    }

    private static String decryptRaw(String cipherText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, requireInitialized(privateKey), OAEP_SHA256);
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new DecryptionException("Unable to decrypt request payload");
        }
    }

    private static <T> T requireInitialized(T value) {
        if (value == null) {
            throw new IllegalStateException("RSAUtil has not been initialized");
        }
        return value;
    }
}

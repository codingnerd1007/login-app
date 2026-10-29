package com.example.loginapp.util;

import com.example.loginapp.exception.DecryptionException;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

/**
 * RSA-OAEP (SHA-256) decryption of values encrypted by the Angular frontend with Web Crypto.
 * Only the public key is given to the frontend; the private key stays on the server and is
 * loaded from the PKCS12 keystore at startup by RsaKeyStoreConfig.
 */
public final class RSAUtil {

    private static final String ALGORITHM = "RSA/ECB/OAEPPadding";

    // Explicit SHA-256 for both the OAEP digest and MGF1. Java's "OAEPWithSHA-256AndMGF1Padding"
    // defaults MGF1 to SHA-1, which does not match Web Crypto's RSA-OAEP with SHA-256.
    private static final OAEPParameterSpec OAEP_SHA256 = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

    private static volatile PrivateKey privateKey;
    private static volatile PublicKey publicKey;

    private RSAUtil() {
    }

    public static void init(PrivateKey privateKey, PublicKey publicKey) {
        RSAUtil.privateKey = privateKey;
        RSAUtil.publicKey = publicKey;
    }

    /** Public key in X.509 SubjectPublicKeyInfo (SPKI) form, Base64 encoded, for the frontend. */
    public static String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(requireInitialized(publicKey).getEncoded());
    }

    public static String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, requireInitialized(privateKey), OAEP_SHA256);
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new DecryptionException("Unable to decrypt request payload");
        }
    }

    private static <T> T requireInitialized(T key) {
        if (key == null) {
            throw new IllegalStateException("RSA keys have not been loaded");
        }
        return key;
    }
}

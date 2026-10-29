package com.example.loginapp.service;

import com.example.loginapp.exception.NonceUnavailableException;
import com.example.loginapp.exception.ReplayDetectedException;
import com.example.loginapp.util.RSAUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Issues single-use nonces that the frontend puts inside every RSA-encrypted value
 * ("nonce:value"). RSAUtil.decrypt consumes the nonce, so a captured ciphertext
 * cannot be sent again.
 */
@Service
public class NonceService {

    private static final int NONCE_BYTES = 16;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Instant> nonceStore = new ConcurrentHashMap<>();

    @Value("${app.nonce.expiry-seconds}")
    private long expirySeconds;

    @Value("${app.nonce.max-active}")
    private int maxActive;

    @PostConstruct
    public void registerWithRsaUtil() {
        RSAUtil.setNonceValidator(this::consumeNonce);
    }

    public String generateNonce() {
        if (nonceStore.size() >= maxActive) {
            removeExpired();
            if (nonceStore.size() >= maxActive) {
                throw new NonceUnavailableException("Too many pending requests. Please try again shortly.");
            }
        }
        byte[] bytes = new byte[NONCE_BYTES];
        random.nextBytes(bytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        nonceStore.put(nonce, Instant.now().plusSeconds(expirySeconds));
        return nonce;
    }

    /** Removes the nonce; throws if it was never issued, has expired or was already used. */
    public void consumeNonce(String nonce) {
        Instant expiresAt = nonce == null ? null : nonceStore.remove(nonce);
        if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
            throw new ReplayDetectedException("Request has expired or was already used. Please try again.");
        }
    }

    private void removeExpired() {
        Instant now = Instant.now();
        nonceStore.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }
}

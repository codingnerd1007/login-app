package com.example.loginapp;

import com.example.loginapp.exception.NonceUnavailableException;
import com.example.loginapp.exception.ReplayDetectedException;
import com.example.loginapp.service.NonceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NonceServiceTest {

    private NonceService nonceService;

    @BeforeEach
    void setUp() {
        nonceService = new NonceService();
        ReflectionTestUtils.setField(nonceService, "expirySeconds", 120L);
        ReflectionTestUtils.setField(nonceService, "maxActive", 3);
    }

    @Test
    void nonceIsSingleUse() {
        String nonce = nonceService.generateNonce();
        assertThatNoException().isThrownBy(() -> nonceService.consumeNonce(nonce));
        assertThatThrownBy(() -> nonceService.consumeNonce(nonce)).isInstanceOf(ReplayDetectedException.class);
    }

    @Test
    void noncesAreUniqueAndUrlSafe() {
        String first = nonceService.generateNonce();
        String second = nonceService.generateNonce();
        assertThat(first).isNotEqualTo(second).hasSize(22).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void expiredNonceIsRejected() {
        ReflectionTestUtils.setField(nonceService, "expirySeconds", -1L);
        String nonce = nonceService.generateNonce();
        assertThatThrownBy(() -> nonceService.consumeNonce(nonce)).isInstanceOf(ReplayDetectedException.class);
    }

    @Test
    void unknownOrNullNonceIsRejected() {
        assertThatThrownBy(() -> nonceService.consumeNonce("never-issued")).isInstanceOf(ReplayDetectedException.class);
        assertThatThrownBy(() -> nonceService.consumeNonce(null)).isInstanceOf(ReplayDetectedException.class);
    }

    @Test
    void stopsIssuingAtTheLimitUntilNoncesAreUsedOrExpire() {
        String first = nonceService.generateNonce();
        nonceService.generateNonce();
        nonceService.generateNonce();
        assertThatThrownBy(nonceService::generateNonce).isInstanceOf(NonceUnavailableException.class);

        nonceService.consumeNonce(first);
        assertThatNoException().isThrownBy(nonceService::generateNonce);
    }

    @Test
    void expiredNoncesAreClearedWhenTheLimitIsReached() {
        ReflectionTestUtils.setField(nonceService, "expirySeconds", -1L);
        nonceService.generateNonce();
        nonceService.generateNonce();
        nonceService.generateNonce();
        ReflectionTestUtils.setField(nonceService, "expirySeconds", 120L);
        assertThatNoException().isThrownBy(nonceService::generateNonce);
    }
}

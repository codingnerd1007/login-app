package com.example.loginapp;

import com.example.loginapp.service.CaptchaService;
import com.example.loginapp.service.EmailService;
import com.example.loginapp.util.AESUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private CaptchaService captchaService;

    @MockBean
    private EmailService emailService;

    @Test
    void aesRoundTrip() {
        String encrypted = AESUtil.encrypt("Admin@123");
        org.assertj.core.api.Assertions.assertThat(AESUtil.decrypt(encrypted)).isEqualTo("Admin@123");
    }

    @Test
    void fullLoginFlowWithCaptchaPasswordAndOtp() throws Exception {
        String captchaId = fetchCaptchaId();
        doNothing().when(captchaService).validateCaptcha(eq(captchaId), anyString());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "admin",
                                "password", AESUtil.encrypt("Admin@123"),
                                "captchaId", captchaId,
                                "captchaText", "anything"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpRequired").value(true))
                .andExpect(jsonPath("$.maskedEmail").value("ad***@example.com"));

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq("admin@example.com"), otpCaptor.capture(), anyLong());

        String body = mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "admin",
                                "otp", AESUtil.encrypt(otpCaptor.getValue())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(body).get("token").asText();
        mockMvc.perform(get("/api/user/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void wrongCaptchaIsRejectedBeforeCredentials() throws Exception {
        String captchaId = fetchCaptchaId();
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "admin",
                                "password", AESUtil.encrypt("Admin@123"),
                                "captchaId", captchaId,
                                "captchaText", "wrong!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CAPTCHA"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        String captchaId = fetchCaptchaId();
        doNothing().when(captchaService).validateCaptcha(eq(captchaId), anyString());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "admin",
                                "password", AESUtil.encrypt("wrong"),
                                "captchaId", captchaId,
                                "captchaText", "anything"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void protectedEndpointRequiresToken() throws Exception {
        mockMvc.perform(get("/api/user/me")).andExpect(status().isUnauthorized());
    }

    private String fetchCaptchaId() throws Exception {
        String body = mockMvc.perform(get("/api/auth/captcha"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("captchaId").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}

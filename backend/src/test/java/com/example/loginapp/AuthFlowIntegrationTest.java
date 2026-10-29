package com.example.loginapp;

import com.example.loginapp.service.CaptchaService;
import com.example.loginapp.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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

    private static final String KEYSTORE_PASSWORD = "test-keystore-password";
    private static final OAEPParameterSpec OAEP_SHA256 = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

    /** Creates a throwaway PKCS12 keystore with keytool, the same way scripts/generate-keystore.sh does. */
    @DynamicPropertySource
    static void rsaKeystore(DynamicPropertyRegistry registry) throws Exception {
        Path dir = Files.createTempDirectory("login-keys");
        Path keystore = dir.resolve("test-keys.p12");
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                "-genkeypair", "-alias", "login-rsa", "-keyalg", "RSA", "-keysize", "2048",
                "-dname", "CN=login-app-test", "-validity", "1",
                "-storetype", "PKCS12", "-keystore", keystore.toString(), "-storepass", KEYSTORE_PASSWORD)
                .redirectErrorStream(true)
                .start();
        if (process.waitFor() != 0) {
            throw new IllegalStateException("keytool failed: " + new String(process.getInputStream().readAllBytes()));
        }
        keystore.toFile().deleteOnExit();
        dir.toFile().deleteOnExit();
        registry.add("app.rsa.keystore.path", keystore::toString);
        registry.add("app.rsa.keystore.password", () -> KEYSTORE_PASSWORD);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private CaptchaService captchaService;

    @MockBean
    private EmailService emailService;

    private PublicKey publicKey;

    @BeforeEach
    void fetchPublicKey() throws Exception {
        String body = mockMvc.perform(get("/api/auth/public-key"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        byte[] spki = Base64.getDecoder().decode(objectMapper.readTree(body).get("publicKey").asText());
        publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(spki));
    }

    @Test
    void publicKeyIsRsa2048() {
        assertThat(publicKey.getAlgorithm()).isEqualTo("RSA");
        assertThat(((java.security.interfaces.RSAPublicKey) publicKey).getModulus().bitLength()).isEqualTo(2048);
    }

    @Test
    void sameValueEncryptsDifferentlyEachTime() throws Exception {
        assertThat(encrypt("Admin@123")).isNotEqualTo(encrypt("Admin@123"));
    }

    @Test
    void sha1MgfCiphertextIsRejected() throws Exception {
        // Java's default OAEPWithSHA-256AndMGF1Padding uses SHA-1 for MGF1; it must not be accepted
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        String wrongPadding = Base64.getEncoder().encodeToString(cipher.doFinal("Admin@123".getBytes(StandardCharsets.UTF_8)));
        String captchaId = fetchCaptchaId();
        doNothing().when(captchaService).validateCaptcha(eq(captchaId), anyString());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "admin",
                                "password", wrongPadding,
                                "captchaId", captchaId,
                                "captchaText", "anything"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("DECRYPTION_FAILED"));
    }

    @Test
    void plainTextPasswordIsRejected() throws Exception {
        String captchaId = fetchCaptchaId();
        doNothing().when(captchaService).validateCaptcha(eq(captchaId), anyString());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "admin",
                                "password", "Admin@123",
                                "captchaId", captchaId,
                                "captchaText", "anything"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("DECRYPTION_FAILED"));
    }

    @Test
    void fullLoginFlowWithCaptchaPasswordAndOtp() throws Exception {
        String captchaId = fetchCaptchaId();
        doNothing().when(captchaService).validateCaptcha(eq(captchaId), anyString());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "admin",
                                "password", encrypt("Admin@123"),
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
                                "otp", encrypt(otpCaptor.getValue())))))
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
                                "password", encrypt("Admin@123"),
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
                                "password", encrypt("wrong"),
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

    /** Encrypts like the frontend: Web Crypto RSA-OAEP with SHA-256, Base64 output. */
    private String encrypt(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SHA256);
        return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}

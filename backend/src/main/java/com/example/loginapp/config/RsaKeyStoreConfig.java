package com.example.loginapp.config;

import com.example.loginapp.util.RSAUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 * Loads the RSA key pair from the PKCS12 keystore at startup and hands it to RSAUtil.
 * Startup fails if the keystore, password or alias is wrong, so the app never runs without keys.
 */
@Slf4j
@Configuration
public class RsaKeyStoreConfig {

    @Value("${app.rsa.keystore.path}")
    private String keystorePath;

    @Value("${app.rsa.keystore.password}")
    private char[] keystorePassword;

    @Value("${app.rsa.keystore.alias}")
    private String keyAlias;

    @PostConstruct
    public void loadKeys() throws Exception {
        Path path = Path.of(keystorePath);
        if (!Files.isReadable(path)) {
            throw new IllegalStateException("RSA keystore not found at " + path.toAbsolutePath()
                    + ". Run scripts/generate-keystore.sh or set LOGIN_KEYSTORE_PATH.");
        }

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(path)) {
            keyStore.load(in, keystorePassword);
        }

        if (!(keyStore.getKey(keyAlias, keystorePassword) instanceof PrivateKey privateKey)) {
            throw new IllegalStateException("No RSA private key with alias '" + keyAlias + "' in " + path);
        }
        Certificate certificate = keyStore.getCertificate(keyAlias);
        RSAUtil.init(privateKey, certificate.getPublicKey());
        log.info("Loaded RSA key '{}' from {}", keyAlias, path);
    }
}

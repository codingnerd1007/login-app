package com.example.loginapp.service;

import com.example.loginapp.dto.CaptchaResponseDto;
import com.example.loginapp.exception.InvalidCaptchaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates a text captcha image and keeps the expected answer in memory against a captcha id.
 * Each captcha can be validated only once.
 */
@Service
public class CaptchaService {

    // Characters that are easy to confuse (0/O, 1/l/I) are left out
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int WIDTH = 160;
    private static final int HEIGHT = 50;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, CaptchaEntry> captchaStore = new ConcurrentHashMap<>();

    @Value("${app.captcha.length}")
    private int captchaLength;

    @Value("${app.captcha.expiry-seconds}")
    private long expirySeconds;

    public CaptchaResponseDto generateCaptcha() {
        removeExpired();
        String text = randomText();
        String captchaId = UUID.randomUUID().toString();
        captchaStore.put(captchaId, new CaptchaEntry(text, Instant.now().plusSeconds(expirySeconds)));
        return new CaptchaResponseDto(captchaId, "data:image/png;base64," + renderImage(text));
    }

    public void validateCaptcha(String captchaId, String captchaText) {
        CaptchaEntry entry = captchaId == null ? null : captchaStore.remove(captchaId);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            throw new InvalidCaptchaException("Captcha has expired. Please try again.");
        }
        if (captchaText == null || !entry.text().equals(captchaText.trim())) {
            throw new InvalidCaptchaException("Invalid captcha");
        }
    }

    private String randomText() {
        StringBuilder sb = new StringBuilder(captchaLength);
        for (int i = 0; i < captchaLength; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private String renderImage(String text) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(240, 243, 248));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // Noise lines
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 8; i++) {
                g.setColor(randomColor(120, 200));
                g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }

            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
            int x = 12;
            for (char c : text.toCharArray()) {
                double angle = Math.toRadians(random.nextInt(31) - 15);
                int y = 34 + random.nextInt(8) - 4;
                g.rotate(angle, x, y);
                g.setColor(randomColor(20, 110));
                g.drawString(String.valueOf(c), x, y);
                g.rotate(-angle, x, y);
                x += 23;
            }

            // Noise dots
            for (int i = 0; i < 80; i++) {
                image.setRGB(random.nextInt(WIDTH), random.nextInt(HEIGHT), randomColor(100, 220).getRGB());
            }
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate captcha image", e);
        }
    }

    private Color randomColor(int min, int max) {
        return new Color(min + random.nextInt(max - min), min + random.nextInt(max - min), min + random.nextInt(max - min));
    }

    private void removeExpired() {
        Instant now = Instant.now();
        captchaStore.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }

    private record CaptchaEntry(String text, Instant expiresAt) {
    }
}

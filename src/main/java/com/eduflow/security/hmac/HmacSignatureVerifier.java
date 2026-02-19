package com.eduflow.security.hmac;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Slf4j
@Component
public class HmacSignatureVerifier {

    @Value("${payment.gateway.webhook-secret}")
    private String webhookSecret;

    private static final String HMAC_SHA256 = "HmacSHA256";

    public boolean verifySignature(String payload, String signature) {
        if (payload == null || signature == null || webhookSecret == null || webhookSecret.isEmpty()) {
            log.warn("Missing parameters for HMAC verification");
            return false;
        }

        try {
            String expectedSignature = calculateHmacSignature(payload);
            boolean isValid = MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );

            if (!isValid) {
                log.warn("HMAC signature verification failed");
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error verifying HMAC signature: {}", e.getMessage());
            return false;
        }
    }

    public String calculateHmacSignature(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC signature", e);
        }
    }

    public boolean verifyTimestamp(long timestamp, long toleranceSeconds) {
        long currentTime = System.currentTimeMillis() / 1000;
        long timeDifference = Math.abs(currentTime - timestamp);
        return timeDifference <= toleranceSeconds;
    }
}

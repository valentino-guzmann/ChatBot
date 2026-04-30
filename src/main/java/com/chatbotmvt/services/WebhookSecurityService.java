package com.chatbotmvt.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class WebhookSecurityService {

    @Value("${facebook.app-secret}")
    private String appSecret;

    public boolean isSignatureValid(String payload, String signature) {
        if (signature == null || !signature.startsWith("sha256=")) {
            return false;
        }

        try {
            String expectedHash = signature.substring(7);

            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);

            byte[] hashBytes = sha256Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String actualHash = HexFormat.of().formatHex(hashBytes);

            return expectedHash.equalsIgnoreCase(actualHash);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return false;
        }
    }
}
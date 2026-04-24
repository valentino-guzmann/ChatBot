package com.chatbotmvt.services;

import com.chatbotmvt.dto.SendMessageRequest;
import com.chatbotmvt.dto.Text;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsappService {

    private final RestClient restClient;

    @Value("${whatsapp.phone-id}")
    private String phoneNumberId;

    @Value("${access.token}")
    private String accessToken;

    public void sendMessage(String phone, String message) {

        if (phone == null || phone.isBlank()) {
            log.warn("⚠️ phone vacío");
            return;
        }

        if (message == null || message.isBlank()) {
            log.warn("⚠️ message vacío");
            return;
        }

        log.info("📤 Enviando WhatsApp -> phone: {}, message: {}", phone, message);

        var request = Map.of(
                "messaging_product", "whatsapp",
                "to", phone,
                "type", "template",
                "template", Map.of(
                        "name", "hello_world",
                        "language", Map.of("code", "en_US")
                )
        );

        try {
            restClient.post()
                    .uri("/{phoneId}/messages", phoneNumberId)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            log.info("✅ Mensaje enviado correctamente");

        } catch (Exception e) {
            log.error("❌ Error enviando WhatsApp message: {}", e.getMessage());
        }
    }
}
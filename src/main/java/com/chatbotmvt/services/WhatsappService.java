package com.chatbotmvt.services;

import com.chatbotmvt.dto.SendMessageRequest;
import com.chatbotmvt.dto.Text;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsappService {

    private final RestClient restClient;

    @Value("${whatsapp.phone-id}")
    private String phoneNumberId;

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

        var request = new SendMessageRequest(
                "whatsapp",
                phone,
                "text",
                new Text(message)
        );

        try {
            restClient.post()
                    .uri("/{phoneId}/messages", phoneNumberId)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            log.info("✅ Mensaje enviado correctamente");

        } catch (Exception e) {
            log.error("❌ Error enviando WhatsApp message: {}", e.getMessage());
        }
    }
}
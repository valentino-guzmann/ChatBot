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

    // 👋 TEMPLATE SALUDO
    public void sendSaludoTemplate(String phone) {

        phone = formatPhone(phone);

        var request = Map.of(
                "messaging_product", "whatsapp",
                "to", phone,
                "type", "template",
                "template", Map.of(
                        "name", "saludo",
                        "language", Map.of("code", "es_AR")
                )
        );

        try {
            restClient.post()
                    .uri("/{phoneId}/messages", phoneNumberId)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            log.info("✅ Template saludo enviado");

        } catch (Exception e) {
            log.error("❌ Error enviando template saludo: {}", e.getMessage());
        }
    }

    // 💬 MENSAJE NORMAL
    public void sendMessage(String phone, String message) {

        if (phone == null || phone.isBlank()) return;
        if (message == null || message.isBlank()) return;

        phone = formatPhone(phone);

        var request = new SendMessageRequest(
                "whatsapp",
                phone,
                "text",
                new Text(message)
        );

        try {
            restClient.post()
                    .uri("/{phoneId}/messages", phoneNumberId)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            log.info("✅ Mensaje enviado");

        } catch (Exception e) {
            log.error("❌ Error enviando mensaje: {}", e.getMessage());
        }
    }

    private String formatPhone(String phone) {
        if (phone.startsWith("549")) {
            return "54" + phone.substring(3);
        }
        return phone;
    }
}
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

    // 👋 TEMPLATE
    public void sendSaludoTemplate(String phone) {

        phone = formatPhone(phone);

        var request = Map.of(
                "messaging_product", "whatsapp",
                "to", phone,
                "type", "template",
                "template", Map.of(
                        "name", "saludo",
                        "language", Map.of("code", "es")
                )
        );

        send(request);
    }

    // 💬 TEXTO NORMAL
    public void sendMessage(String phone, String message) {

        phone = formatPhone(phone); // 🔥 FIX CLAVE

        var request = new SendMessageRequest(
                "whatsapp",
                phone,
                "text",
                new Text(message)
        );

        send(request);
    }

    // 🔥 MÉTODO CENTRALIZADO
    private void send(Object body) {
        try {
            restClient.post()
                    .uri("/{phoneId}/messages", phoneNumberId)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("✅ Mensaje enviado");

        } catch (Exception e) {
            log.error("❌ Error WhatsApp: {}", e.getMessage());
        }
    }

    private String formatPhone(String phone) {
        if (phone.startsWith("549")) {
            return "54" + phone.substring(3);
        }
        return phone;
    }
}
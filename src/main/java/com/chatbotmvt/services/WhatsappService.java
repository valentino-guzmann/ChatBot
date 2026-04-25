package com.chatbotmvt.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Service @RequiredArgsConstructor
public class WhatsappService {
    private final RestClient restClient;
    @Value("${whatsapp.phone-id}") private String phoneNumberId;
    @Value("${access.token}") private String accessToken;

    public void sendMessage(String phone, String message) {
        var body = Map.of("messaging_product", "whatsapp", "to", phone,
                "type", "text", "text", Map.of("body", message));
        execute(body);
    }

    public void sendSaludo(String phone) {
        var body = Map.of(
                "messaging_product", "whatsapp",
                "to", phone,
                "type", "template",
                "template", Map.of(
                        "name", "saludo",
                        "language", Map.of("code", "es_AR")
                )
        );
        execute(body);
    }

    private void execute(Object body) {
        try {
            restClient.post().uri("/{id}/messages", phoneNumberId)
                    .header("Authorization", "Bearer " + accessToken).body(body)
                    .retrieve().toBodilessEntity();
        } catch (Exception ignored) {}
    }
}
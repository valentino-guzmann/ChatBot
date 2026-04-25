package com.chatbotmvt.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WhatsappService {

    private final RestClient restClient;

    @Value("${whatsapp.phone-id}")
    private String phoneNumberId;

    @Value("${access.token}")
    private String accessToken;

    public void sendMessage(String phone, String message) {
        var body = Map.of(
                "messaging_product", "whatsapp",
                "to", formatPhone(phone),
                "type", "text",
                "text", Map.of("body", message)
        );

        execute(body);
    }

    public void sendTemplate(String phone, String templateName) {
        var body = Map.of(
                "messaging_product", "whatsapp",
                "to", formatPhone(phone),
                "type", "template",
                "template", Map.of(
                        "name", templateName,
                        "language", Map.of("code", "es_AR")
                )
        );

        execute(body);
    }

    public void sendSaludoSeguro(String phone) {
        try {
            sendTemplate(phone, "saludo");
        } catch (Exception e) {
            System.err.println("❌ Template falló, uso mensaje normal");

            sendMessage(phone,
                    "👋 Hola! Bienvenido a Servicios Públicos.\n\nEscribe cualquier mensaje para comenzar.");
        }
    }

    private void execute(Object body) {
        try {
            restClient.post()
                    .uri("/{id}/messages", phoneNumberId)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception e) {
            System.err.println("❌ Error enviando mensaje: " + e.getMessage());
            throw e;
        }
    }

    private String formatPhone(String p) {
        return p.startsWith("549") ? "54" + p.substring(3) : p;
    }
}
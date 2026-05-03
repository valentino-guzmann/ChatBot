package com.chatbotmvt.services;

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

        log.info("📤 Enviando mensaje de texto a [{}]", phone);
        log.debug("📝 Contenido mensaje: {}", message);

        var body = Map.of(
                "messaging_product", "whatsapp",
                "to", formatPhone(phone),
                "type", "text",
                "text", Map.of("body", message)
        );

        execute(body);
    }

    public void sendImage(String phone, String imageUrl) {
        log.info("📤 Enviando imagen a [{}]", phone);
        var body = Map.of(
                "messaging_product", "whatsapp",
                "to", formatPhone(phone),
                "type", "image",
                "image", Map.of("link", imageUrl)
        );
        execute(body);
    }

    private void execute(Object body) {

        log.debug("🚀 Ejecutando request a WhatsApp API");

        try {
            restClient.post()
                    .uri("/{id}/messages", phoneNumberId)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("✅ Mensaje enviado correctamente");

        } catch (Exception e) {

            log.error("❌ Error enviando mensaje a WhatsApp: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String formatPhone(String p) {

        String formatted = p.startsWith("549") ? "54" + p.substring(3) : p;

        log.debug("📱 Número formateado: {} → {}", p, formatted);

        return formatted;
    }
}
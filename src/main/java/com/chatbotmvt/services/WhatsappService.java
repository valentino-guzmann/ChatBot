package com.chatbotmvt.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public void sendTemplate(String phone, String templateName, String mediaId, String bodyText) {

        log.info("📤 Enviando Template [{}] a [{}]", templateName, phone);

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("name", templateName);
        templateData.put("language", Map.of("code", "es_AR"));

        List<Map<String, Object>> components = new ArrayList<>();

        if (mediaId != null && !mediaId.isBlank()) {
            components.add(Map.of(
                    "type", "header",
                    "parameters", List.of(
                            Map.of("type", "image", "image", Map.of("id", mediaId))
                    )
            ));
        }

        if (bodyText != null && !bodyText.isBlank()) {
            components.add(Map.of(
                    "type", "body",
                    "parameters", List.of(
                            Map.of("type", "text", "text", bodyText)
                    )
            ));
        }

        templateData.put("components", components);

        var body = Map.of(
                "messaging_product", "whatsapp",
                "to", formatPhone(phone),
                "type", "template",
                "template", templateData
        );

        execute(body);
    }

    private void execute(Object body) {

        log.debug("🚀 Ejecutando request a WhatsApp API");

        try {
            long start = System.currentTimeMillis();

            restClient.post()
                    .uri("/{id}/messages", phoneNumberId)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            long end = System.currentTimeMillis();
            log.info("✅ Mensaje enviado correctamente");

            log.info("⏱️ Tiempo API WhatsApp: {} ms", (end - start));

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
package com.chatbotmvt.services;

import com.chatbotmvt.dto.WhatsappResponse;
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

    public String sendMessage(String phone, String message) {

        log.info("📤 Enviando mensaje de texto a [{}]", phone);
        log.debug("📝 Contenido mensaje: {}", message);

        var body = Map.of(
                "messaging_product", "whatsapp",
                "to", formatPhone(phone),
                "type", "text",
                "text", Map.of("body", message)
        );

        return execute(body);
    }

    public String sendTemplate(String phone, String templateName, String mediaId, String bodyText) {
        log.info("📤 Enviando Template [{}] a [{}]", templateName, phone);
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("name", templateName);
        templateData.put("language", Map.of("code", "es_AR"));

        List<Map<String, Object>> components = new ArrayList<>();

        if (mediaId != null && !mediaId.isBlank()) {
            components.add(Map.of(
                    "type", "header",
                    "parameters", List.of(Map.of("type", "image", "image", Map.of("id", mediaId)))
            ));
        }

        if (bodyText != null && !bodyText.isBlank()) {
            components.add(Map.of(
                    "type", "body",
                    "parameters", List.of(Map.of("type", "text", "text", bodyText))
            ));
        }

        if (!components.isEmpty()) templateData.put("components", components);

        var body = Map.of(
                "messaging_product", "whatsapp",
                "to", phone,
                "type", "template",
                "template", templateData
        );
        return execute(body);
    }

    public String sendImageById(String phone, String mediaId, String caption) {
        log.info("📤 Enviando Imagen con Media ID [{}]", mediaId);
        var body = Map.of(
                "messaging_product", "whatsapp",
                "to", phone,
                "type", "image",
                "image", Map.of(
                        "id", mediaId,
                        "caption", caption != null ? caption : ""
                )
        );
        return execute(body);
    }

    private String execute(Object body) {

        log.debug("🚀 Ejecutando request a WhatsApp API");

        try {
            var response = restClient.post()
                    .uri("/{id}/messages", phoneNumberId)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(WhatsappResponse.class);

            if (response != null && response.messages() != null && !response.messages().isEmpty()) {
                String wamid = response.messages().get(0).get("id");
                log.info("✅ Mensaje enviado. ID: {}", wamid);
                return wamid;
            }
        } catch (Exception e) {
            log.error("❌ Error enviando a WhatsApp: {}", e.getMessage());
        }
        return null;
    }

    private String formatPhone(String p) {
        return p;
    }

}
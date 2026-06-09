package com.chatbotmvt.services;

import com.chatbotmvt.dto.WhatsappResponse;
import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.repository.BotStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsappService {

    private final RestClient restClient;
    private final BotStateRepository botStateRepository;

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

    public String sendImageWithAutoRefresh(String phone, BotState state, String caption) {
        try {
            return sendImageById(phone, state.getMediaId(), caption);
        } catch (HttpClientErrorException e) {
            if (!isExpiredMediaError(e)) {
                throw e;
            }
            log.warn("🔁 Media ID expirado para estado [{}]. Re-subiendo desde [{}]", state.getName(), state.getMediaPath());
            String newMediaId = refreshMediaId(state);
            return sendImageById(phone, newMediaId, caption);
        }
    }

    private String refreshMediaId(BotState state) {
        if (state.getMediaPath() == null || state.getMediaPath().isBlank()) {
            throw new IllegalStateException("El estado " + state.getName() + " no tiene mediaPath configurado");
        }
        if (state.getMediaMimeType() == null || state.getMediaMimeType().isBlank()) {
            throw new IllegalStateException("El estado " + state.getName() + " no tiene mediaMimeType configurado");
        }

        ClassPathResource resource = new ClassPathResource("static/" + state.getMediaPath());

        try {
            byte[] bytes = resource.getInputStream().readAllBytes();
            String newMediaId = uploadMedia(bytes, state.getMediaPath(), state.getMediaMimeType());
            state.setMediaId(newMediaId);
            botStateRepository.save(state);
            log.info("✅ Nuevo Media ID [{}] guardado para estado [{}]", newMediaId, state.getName());
            return newMediaId;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el recurso estático: " + state.getMediaPath(), e);
        }
    }

    private String uploadMedia(byte[] bytes, String filename, String mimeType) {
        ByteArrayResource fileResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename.substring(filename.lastIndexOf('/') + 1);
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("messaging_product", "whatsapp");
        body.add("type", mimeType);
        body.add("file", fileResource);

        Map<String, Object> response = restClient.post()
                .uri("/{id}/media", phoneNumberId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("id") == null) {
            throw new IllegalStateException("WhatsApp no devolvió media id al subir el archivo " + filename);
        }

        return response.get("id").toString();
    }

    private boolean isExpiredMediaError(HttpClientErrorException e) {
        String body = e.getResponseBodyAsString();
        return e.getStatusCode().value() == 400
                && body.contains("\"code\":131009")
                && body.contains("does not exist or has expired");
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
        } catch (HttpClientErrorException e) {
            log.error("❌ Error enviando a WhatsApp: {}", e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("❌ Error enviando a WhatsApp: {}", e.getMessage(), e);
        }
        return null;
    }

    private String formatPhone(String p) {
        return p;
    }

}

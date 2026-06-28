package com.chatbotmvt.services;

import com.chatbotmvt.dto.WhatsappResponse;
import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.repository.BotStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsappService {

    private static final String GITHUB_RAW_BASE =
            "https://raw.githubusercontent.com/valentino-guzmann/ChatBot/main/src/main/resources/static/";

    private final RestClient restClient;
    private final BotStateRepository botStateRepository;

    @Value("${whatsapp.phone-id}")
    private String phoneNumberId;

    @Value("${access.token}")
    private String accessToken;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:}")
    private String baseUrl;

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

    public String sendTemplateWithAutoRefresh(String phone, BotState state, String bodyText) {
        try {
            return sendTemplate(phone, state.getTemplateName(), state.getMediaId(), bodyText);
        } catch (HttpClientErrorException e) {
            if (isMediaIdError(e)) {
                log.warn("🔁 Media ID inválido o expirado en template [{}] para estado [{}]. Re-subiendo desde [{}]",
                        state.getTemplateName(), state.getName(), state.getMediaPath());
                String newMediaId = refreshMediaId(state);
                return sendTemplate(phone, state.getTemplateName(), newMediaId, bodyText);
            }
            throw e;
        }
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
            if (isMediaIdError(e)) {
                log.warn("🔁 Media ID inválido o expirado para estado [{}]. Re-subiendo desde [{}]", state.getName(), state.getMediaPath());
                String newMediaId = refreshMediaId(state);
                return sendImageById(phone, newMediaId, caption);
            }
            throw e;
        }
    }

    private String refreshMediaId(BotState state) {
        if (state.getMediaPath() == null || state.getMediaPath().isBlank()) {
            throw new IllegalStateException("El estado " + state.getName() + " no tiene mediaPath configurado");
        }
        if (state.getMediaMimeType() == null || state.getMediaMimeType().isBlank()) {
            throw new IllegalStateException("El estado " + state.getName() + " no tiene mediaMimeType configurado");
        }

        String url = GITHUB_RAW_BASE + state.getMediaPath();
        log.info("🌐 Descargando imagen desde GitHub Raw: {}", url);

        try {
            byte[] bytes;
            try (InputStream is = URI.create(url).toURL().openStream()) {
                bytes = is.readAllBytes();
            }
            log.info("💾 Imagen descargada: {} bytes", bytes.length);
            String newMediaId = uploadMedia(bytes, state.getMediaPath(), state.getMediaMimeType());
            state.setMediaId(newMediaId);
            botStateRepository.save(state);
            log.info("✅ Nuevo Media ID [{}] guardado para estado [{}]", newMediaId, state.getName());
            return newMediaId;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo descargar la imagen desde GitHub Raw: " + url, e);
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

    private boolean isMediaIdError(HttpClientErrorException e) {
        if (e.getStatusCode().value() != 400) return false;
        String body = e.getResponseBodyAsString().toLowerCase();
        // Error de media expirado (code 131009)
        if (body.contains("\"code\":131009") && body.contains("does not exist or has expired")) return true;
        // Error de media ID inválido (code 100) - "not a valid whatsapp business account media attachment ID"
        if (body.contains("\"code\":100") && body.contains("media attachment id")) return true;
        // Error genérico de image.id no válido
        if (body.contains("image.id") && body.contains("not valid")) return true;
        return false;
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

    public MediaInfo getMediaInfo(String mediaId) {
        log.info("📥 Obteniendo info de media [{}]", mediaId);
        try {
            var response = restClient.get()
                    .uri("/{mediaId}", mediaId)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("url") == null) {
                throw new IllegalStateException("WhatsApp no devolvió URL de descarga para mediaId: " + mediaId);
            }

            return new MediaInfo(
                    response.get("url").toString(),
                    response.get("mime_type") != null ? response.get("mime_type").toString() : null,
                    response.get("sha256") != null ? response.get("sha256").toString() : null
            );
        } catch (HttpClientErrorException e) {
            log.error("❌ Error obteniendo media info: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public byte[] downloadMediaBytes(String downloadUrl) {
        log.info("📥 Descargando media desde URL");
        try {
            return RestClient.builder()
                    .build()
                    .get()
                    .uri(downloadUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception e) {
            log.error("❌ Error descargando media: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo descargar la imagen", e);
        }
    }

    public String downloadAndSaveImage(String mediaId, String phone) {
        try {
            MediaInfo info = getMediaInfo(mediaId);
            byte[] bytes = downloadMediaBytes(info.url());

            String extension = getExtensionFromMimeType(info.mimeType());
            String filename = phone + "_" + UUID.randomUUID() + extension;

            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(filename);
            Files.write(filePath, bytes, StandardOpenOption.CREATE);

            log.info("✅ Imagen guardada en: {}", filePath);
            return "/uploads/" + filename;
        } catch (IOException e) {
            log.error("❌ Error guardando imagen: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo guardar la imagen", e);
        }
    }

    private String getExtensionFromMimeType(String mimeType) {
        if (mimeType == null) return ".jpg";
        return switch (mimeType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    public record MediaInfo(String url, String mimeType, String sha256) {}
}

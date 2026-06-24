package com.chatbotmvt.controller;

import com.chatbotmvt.dto.*;
import com.chatbotmvt.repository.MensajeLogRepository;
import com.chatbotmvt.services.BotService;
import com.chatbotmvt.services.WebhookSecurityService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final BotService botService;
    private final WebhookSecurityService securityService;
    private final ObjectMapper objectMapper;
    private final MensajeLogRepository mensajeLogRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Cache<String, Boolean> processedMessagesCache;

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.challenge", required = false) String challenge,
            @RequestParam(value = "hub.verify_token", required = false) String token
    ) {
        log.info("🔐 Verificación webhook → mode={}, token={}", mode, token);
        if ("subscribe".equals(mode) && "mi_token_secreto".equals(token)) {
            log.info("✅ Webhook verificado");
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token inválido");
    }

    @PostMapping
    public ResponseEntity<Void> receiveMessage(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawPayload) {

        log.info("📥 POST /webhook recibido");
        ResponseEntity<Void> response = ResponseEntity.ok().build();
        CompletableFuture.runAsync(() -> processWebhookAsync(signature, rawPayload));
        return response;
    }

    private void processWebhookAsync(String signature, String rawPayload) {
        try {
            if (signature != null && !signature.isBlank()) {
                if (!securityService.isSignatureValid(rawPayload, signature)) {
                    log.error("❌ Firma inválida. Revisa APP_SECRET");
                    return;
                }
            }

            WebhookRequest request = objectMapper.readValue(rawPayload, WebhookRequest.class);
            if (request.entry() == null || request.entry().isEmpty()) return;

            Value value = request.entry().get(0).changes().get(0).value();

            if (value.statuses() != null && !value.statuses().isEmpty()) {
                Status s = value.statuses().get(0);
                String wamid = s.id();
                String statusStr = s.status();

                log.info("📊 Status Update recibido: ID [{}] → Estado [{}]", wamid, statusStr);

                mensajeLogRepository.findByMessageId(wamid).ifPresent(msg -> {
                    msg.setStatus(statusStr);
                    mensajeLogRepository.save(msg);

                    messagingTemplate.convertAndSend("/topic/updates", "status_update");
                    log.debug("✅ Estado actualizado en BD para mensaje {}", wamid);
                });
                return;
            }

            if (value.messages() != null && !value.messages().isEmpty()) {
                MessageReceived msg = value.messages().get(0);
                String wamid = msg.id();
                String phone = msg.from();
                String type = msg.type();

                if (processedMessagesCache.getIfPresent(wamid) != null) {
                    log.warn("♻️ Mensaje duplicado detectado (wamid: {}). Ignorando.", wamid);
                    return;
                }
                processedMessagesCache.put(wamid, true);

                log.info("📨 Mensaje recibido de [{}], Tipo [{}], ID [{}]", phone, type, wamid);

                if ("text".equals(type) && msg.text() != null) {
                    String text = msg.text().body();
                    log.info("💬 Texto del usuario: \"{}\"", text);
                    botService.procesarYResponder(phone, text);
                } else if ("image".equals(type) && msg.image() != null) {
                    String mediaId = msg.image().id();
                    String mimeType = msg.image().mimeType();
                    String caption = msg.image().caption();
                    log.info("🖼️ Imagen recibida de [{}], mediaId=[{}], mimeType=[{}], caption=[{}]", phone, mediaId, mimeType, caption);
                    botService.procesarImagenEntrante(phone, mediaId, mimeType, caption);
                } else {
                    log.info("⚠ Tipo no manejado directamente: {}.", type);
                }
            }

        } catch (Exception e) {
            log.error("❌ Error procesando webhook asíncronamente: {}", e.getMessage(), e);
        }
    }
}
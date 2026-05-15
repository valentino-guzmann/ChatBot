package com.chatbotmvt.controller;

import com.chatbotmvt.dto.*;
import com.chatbotmvt.services.BotService;
import com.chatbotmvt.services.WhatsappService;
import com.chatbotmvt.services.WebhookSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final BotService botService;
    private final WebhookSecurityService securityService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String token
    ) {
        log.info("🔐 Intento de verificación webhook → token recibido: {}", token);

        if ("subscribe".equals(mode) && "mi_token_secreto".equals(token)) {
            log.info("✅ Webhook verificado correctamente");
            return ResponseEntity.ok(challenge);
        }

        log.warn("❌ Falló verificación webhook");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping
    public ResponseEntity<Void> receiveMessage(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawPayload) {

        log.debug("📥 Payload recibido: {}", rawPayload);

        if (!securityService.isSignatureValid(rawPayload, signature)) {
            log.warn("❌ Firma inválida");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            WebhookRequest request = objectMapper.readValue(rawPayload, WebhookRequest.class);

            var messageOpt = extractMessage(request);

            if (messageOpt.isPresent()) {
                MessageReceived msg = messageOpt.get();

                String phone = msg.from();
                String text = msg.text() != null ? msg.text().body() : null;

                log.info("📩 MENSAJE RECIBIDO → FROM: [{}] | TEXT: [{}]", phone, text);

                if (phone != null && phone.contains("5556289170")) {
                    log.warn("🚫 Mensaje ignorado del TEST NUMBER: {}", phone);
                    return ResponseEntity.ok().build();
                }

                if (text != null && !text.isBlank()) {
                    log.info("⚙️ Enviando a botService...");

                    botService.procesarYResponder(phone, text);
                } else {
                    log.warn("⚠️ Mensaje sin texto ignorado");
                }
            } else {
                log.debug("ℹ️ Webhook recibido sin mensajes válidos (posible status update)");
            }

        } catch (Exception e) {
            log.error("❌ Error procesando webhook: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok().build();
    }

    private Optional<MessageReceived> extractMessage(WebhookRequest request) {
        try {
            return Optional.ofNullable(request.entry())
                    .filter(entries -> !entries.isEmpty())
                    .map(entries -> entries.get(0))
                    .map(Entry::changes)
                    .filter(changes -> !changes.isEmpty())
                    .map(changes -> changes.get(0))
                    .map(Change::value)
                    .map(Value::messages)
                    .filter(messages -> !messages.isEmpty())
                    .map(messages -> messages.get(0));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
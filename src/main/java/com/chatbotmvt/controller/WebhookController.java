package com.chatbotmvt.controller;

import com.chatbotmvt.dto.*;
import com.chatbotmvt.services.BotService;
import com.chatbotmvt.services.WebhookSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            log.debug("📄 Payload: {}", rawPayload);

            if (signature != null && !signature.isBlank()) {
                if (!securityService.isValidSignature(rawPayload, signature)) {
                    log.error("❌ Firma inválida. Revisa APP_SECRET");
                }
            }

            WebhookRequest request = objectMapper.readValue(rawPayload, WebhookRequest.class);

            if (request.entry() == null || request.entry().isEmpty()) return;
            Value value = request.entry().get(0).changes().get(0).value();

            // 1. Si es status update
            if (value.statuses() != null && !value.statuses().isEmpty()) {
                Status s = value.statuses().get(0);
                log.info("📊 Status: {} → {} para {}", s.id(), s.status(), s.recipientId());
                return;
            }

            // 2. Si es mensaje entrante
            if (value.messages() != null && !value.messages().isEmpty()) {
                MessageReceived msg = value.messages().get(0);
                String phone = msg.from();
                String type = msg.type();
                log.info("📨 Mensaje [{}] de [{}]", type, phone);

                if ("text".equals(type) && msg.text() != null) {
                    String text = msg.text().body();
                    log.info("💬 Texto: \"{}\"", text);
                    botService.procesarYResponder(phone, text);
                } else {
                    log.info("⚠ Tipo no manejado: {}. Respondiendo genérico", type);
                    botService.procesarYResponder(phone, "[Recibí tu " + type + "]");
                }
            }

        } catch (Exception e) {
            log.error("❌ Error procesando webhook: {}", e.getMessage(), e);
        }
    }
}
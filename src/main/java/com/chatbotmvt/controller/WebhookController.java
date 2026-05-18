package com.chatbotmvt.controller;

import com.chatbotmvt.dto.*;
import com.chatbotmvt.services.BotService;
import com.chatbotmvt.services.WebhookSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final BotService botService;
    private final WebhookSecurityService securityService;
    private final ObjectMapper objectMapper;

    /**
     * GET: Meta usa esto para verificar que tu webhook existe
     */
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.challenge", required = false) String challenge,
            @RequestParam(value = "hub.verify_token", required = false) String token
    ) {
        log.info("🔐 Verificación webhook → mode={}, token={}", mode, token);

        if ("subscribe".equals(mode) && "mi_token_secreto".equals(token)) {
            log.info("✅ Webhook verificado correctamente");
            return ResponseEntity.ok(challenge);
        }

        log.warn("❌ Falló verificación webhook: mode={}, token={}", mode, token);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token inválido");
    }

    /**
     * POST: Acá llegan TODOS los eventos de WhatsApp: mensajes, status, etc
     */
    @PostMapping
    public ResponseEntity<Void> receiveMessage(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawPayload) {

        // 1. Log inmediato para saber si Meta te pegó
        log.info("📥 POST /webhook recibido");

        // 2. Responder 200 YA, antes de procesar nada. Meta exige respuesta <5s
        ResponseEntity<Void> response = ResponseEntity.ok().build();

        // 3. Procesar async para no bloquear la respuesta
        CompletableFuture.runAsync(() -> processWebhookAsync(signature, rawPayload));

        return response;
    }

    private void processWebhookAsync(String signature, String rawPayload) {
        try {
            log.debug("📄 Payload crudo: {}", rawPayload);

            // 4. Validar firma SOLO si Meta la manda. Si falla, loguear pero no cortar
            if (signature != null && !signature.isBlank()) {
                if (!securityService.isValidSignature(rawPayload, signature)) {
                    log.error("❌ Firma X-Hub-Signature-256 inválida. Revisa tu APP_SECRET");
                } else {
                    log.debug("✅ Firma validada correctamente");
                }
            }

            // 5. Parsear JSON
            WebhookRequest request = objectMapper.readValue(rawPayload, WebhookRequest.class);

            // 6. Ver qué tipo de evento es
            if (request.entry() == null || request.entry().isEmpty()) {
                log.warn("⚠ Webhook sin entries");
                return;
            }

            Value value = request.entry().get(0).changes().get(0).value();

            // 6.1 Si es un status update: sent, delivered, read, failed
            if (value.statuses() != null && !value.statuses().isEmpty()) {
                Status status = value.statuses().get(0);
                log.info("📊 Status update: {} → {}", status.recipientId(), status.status());
                return;
            }

            // 6.2 Si es un mensaje entrante
            Optional<MessageReceived> messageOpt = extractMessage(request);
            if (messageOpt.isPresent()) {
                MessageReceived msg = messageOpt.get();
                String phone = msg.from();
                String type = msg.type();

                log.info("📨 Mensaje tipo [{}] recibido de [{}]", type, phone);

                // Solo procesamos texto por ahora
                if ("text".equals(type) && msg.text() != null) {
                    String text = msg.text().body();
                    log.info("💬 Texto: \"{}\"", text);
                    botService.procesarYResponder(phone, text);
                } else {
                    log.info("⚠ Tipo de mensaje no manejado: {}", type);
                }
            } else {
                log.debug("ℹ Evento sin mensajes. Probablemente un status o cambio de perfil");
            }

        } catch (Exception e) {
            log.error("❌ Error grave procesando webhook: {}", e.getMessage(), e);
        }
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
                    .filter(messages -> messages != null && !messages.isEmpty())
                    .map(messages -> messages.get(0));
        } catch (Exception e) {
            log.error("Error extrayendo mensaje: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
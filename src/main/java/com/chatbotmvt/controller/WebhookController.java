package com.chatbotmvt.controller;

import com.chatbotmvt.dto.*;
import com.chatbotmvt.services.BotService;
import com.chatbotmvt.services.WebhookSecurityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

        log.info("📥 Payload recibido: {}", rawPayload);

//        if (!securityService.isSignatureValid(rawPayload, signature)) {
//            log.warn("❌ Firma inválida");
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
//        }

        try {
            WebhookRequest request = objectMapper.readValue(rawPayload, WebhookRequest.class);

            var messageOpt = extractMessage(request);

            if (messageOpt.isPresent()) {
                MessageReceived msg = messageOpt.get();

                if (msg.text() != null && msg.text().body() != null) {
                    String phone = msg.from();
                    String text = msg.text().body();

                    log.info("📩 Mensaje recibido de [{}]. Procesando de forma asíncrona...", phone);

                    botService.procesarYResponder(phone, text);
                }
            }

        } catch (Exception e) {
            log.error("❌ Error procesando webhook: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    private Optional<MessageReceived> extractMessage(WebhookRequest request) {
        if (request == null || request.entry() == null || request.entry().isEmpty()) {
            log.warn("⚠️ WebhookRequest vacío o sin entries");
            return Optional.empty();
        }

        Entry entry = request.entry().get(0);
        if (entry.changes() == null || entry.changes().isEmpty()) {
            log.warn("⚠️ Entry sin cambios (changes)");
            return Optional.empty();
        }

        Change change = entry.changes().get(0);
        if (change.value() == null || change.value().messages() == null || change.value().messages().isEmpty()) {
            // IMPORTANTE: WhatsApp envía "statuses" (confirmaciones de lectura) que NO traen mensajes.
            // Estos se deben ignorar y no son errores.
            log.info("ℹ️ El cambio no contiene mensajes (puede ser un estado de entrega)");
            return Optional.empty();
        }

        return Optional.of(change.value().messages().get(0));
    }
}
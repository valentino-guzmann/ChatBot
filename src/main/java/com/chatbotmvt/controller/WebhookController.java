package com.chatbotmvt.controller;

import com.chatbotmvt.dto.MessageReceived;
import com.chatbotmvt.dto.WebhookRequest;
import com.chatbotmvt.services.BotService;
import com.chatbotmvt.services.WhatsappService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final BotService botService;
    private final WhatsappService whatsappService;

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String token
    ) {

        log.info("🔐 Verificación webhook → mode: {}, token recibido: {}", mode, token);

        if ("subscribe".equals(mode) && "mi_token_secreto".equals(token)) {
            log.info("✅ Webhook verificado correctamente");
            return ResponseEntity.ok(challenge);
        }

        log.warn("❌ Falló verificación webhook");
        return ResponseEntity.status(403).build();
    }

    @PostMapping
    public ResponseEntity<Void> receiveMessage(@RequestBody WebhookRequest request) {

        log.info("📥 Webhook recibido");

        var messageOpt = extractMessage(request);

        if (messageOpt.isEmpty()) {
            log.warn("⚠️ No se encontró mensaje en el payload");
            return ResponseEntity.ok().build();
        }

        var msg = messageOpt.get();

        if (msg.text() == null || msg.text().body() == null) {
            log.warn("⚠️ Mensaje sin texto válido");
            return ResponseEntity.ok().build();
        }

        String phone = msg.from();
        String text = msg.text().body();

        log.info("📩 Mensaje entrante de [{}]: {}", phone, text);

        try {

            String response = botService.procesarMensaje(phone, text);

            if (response != null) {

                log.info("📤 Enviando respuesta a [{}]", phone);

                whatsappService.sendMessage(phone, response);

            } else {

                log.info("⏭️ No se envía respuesta (flujo controlado, ejemplo: saludo inicial)");
            }

        } catch (Exception e) {

            log.error("❌ Error procesando mensaje de [{}]: {}", phone, e.getMessage(), e);
        }

        return ResponseEntity.ok().build();
    }

    public Optional<MessageReceived> extractMessage(WebhookRequest request) {

        log.debug("🔍 Extrayendo mensaje del payload");

        if (request.entry() == null || request.entry().isEmpty()) {
            log.warn("⚠️ entry vacío");
            return Optional.empty();
        }

        var entry = request.entry().get(0);

        if (entry.changes() == null || entry.changes().isEmpty()) {
            log.warn("⚠️ changes vacío");
            return Optional.empty();
        }

        var change = entry.changes().get(0);

        if (change.value() == null || change.value().messages() == null || change.value().messages().isEmpty()) {
            log.warn("⚠️ messages vacío");
            return Optional.empty();
        }

        log.debug("✅ Mensaje extraído correctamente");

        return Optional.of(change.value().messages().get(0));
    }
}
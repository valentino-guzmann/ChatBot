package com.chatbotmvt.controller;

import com.chatbotmvt.dto.*;
import com.chatbotmvt.services.BotService;
import com.chatbotmvt.services.WebhookSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper; // Asegúrate que este sea el import correcto de tu proyecto

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
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.challenge", required = false) String challenge,
            @RequestParam(value = "hub.verify_token", required = false) String token
    ) {
        log.info("🔐 Intento de verificación webhook → token recibido: {}", token);

        if ("subscribe".equals(mode) && "mi_token_secreto".equals(token)) {
            log.info("✅ Webhook verificado correctamente");
            return ResponseEntity.ok(challenge);
        }

        log.warn("❌ Falló verificación webhook: mode={}, token={}", mode, token);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token de verificación inválido");
    }

    /**
     * MÉTODO POST: Donde llegan los mensajes reales de WhatsApp.
     */
    @PostMapping
    public ResponseEntity<Void> receiveMessage(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawPayload) {

        // 1. LOG CRÍTICO: Ver si llega la petición
        log.info("📥 Petición POST recibida en /webhook");
        log.debug("📄 Payload crudo: {}", rawPayload);

        // 2. VALIDACIÓN DE SEGURIDAD (Firma)
        // Si tienes problemas para probar con Insomnia, puedes comentar este bloque IF temporalmente.
        if (!securityService.isSignatureValid(rawPayload, signature)) {
            log.warn("❌ Firma inválida de Facebook. Firma recibida: {}", signature);
            // Si quieres permitir pruebas externas (Insomnia), cambia esto por un log y no retornes FORBIDDEN.
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            // 3. PARSEAR EL JSON
            WebhookRequest request = objectMapper.readValue(rawPayload, WebhookRequest.class);

            // 4. EXTRAER EL MENSAJE USANDO TU LÓGICA DE DTOS
            Optional<MessageReceived> messageOpt = extractMessage(request);

            if (messageOpt.isPresent()) {
                MessageReceived msg = messageOpt.get();

                String phone = msg.from(); // Aquí verás si viene con 549 o 54
                String text = (msg.text() != null) ? msg.text().body() : null;

                if (phone != null && text != null) {
                    // 5. LOG DE IDENTIFICACIÓN: Aquí verás el número real que envía Meta
                    log.info("📩 Mensaje recibido de [{}]: \"{}\"", phone, text);

                    // 6. PROCESAR ASÍNCRONAMENTE
                    botService.procesarYResponder(phone, text);
                } else {
                    log.warn("⚠️ Se recibió un evento pero no contiene teléfono o texto. Tipo: {}", msg.type());
                }
            } else {
                log.debug("ℹ️ El JSON recibido no es un mensaje de texto (puede ser un estado de entrega: sent, delivered, read)");
            }

        } catch (Exception e) {
            log.error("❌ Error grave procesando el webhook: {}", e.getMessage(), e);
        }

        // Siempre responder 200 OK a Meta para que no reintente el mismo mensaje infinitamente
        return ResponseEntity.ok().build();
    }

    /**
     * Navega por la estructura de Meta para encontrar el primer mensaje de la lista.
     */
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
            log.error("Error extrayendo mensaje del DTO: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
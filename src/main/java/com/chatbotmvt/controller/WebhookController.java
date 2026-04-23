package com.chatbotmvt.controller;

import com.chatbotmvt.dto.MessageReceived;
import com.chatbotmvt.dto.WebhookRequest;
import com.chatbotmvt.services.BotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final BotService botService;

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String token
    ) {

        if ("subscribe".equals(mode) && "mi_token_secreto".equals(token)) {
            return ResponseEntity.ok(challenge);
        }

        return ResponseEntity.status(403).build();
    }

    @PostMapping
    public ResponseEntity<Void> receiveMessage(@RequestBody WebhookRequest request) {

        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📡 WEBHOOK RECIBIDO");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        var messageOpt = extractMessage(request);

        if (messageOpt.isEmpty()) {
            System.out.println("❌ No se encontró mensaje en payload");
            return ResponseEntity.ok().build();
        }

        var msg = messageOpt.get();

        if (msg.text() == null || msg.text().body() == null) {
            System.out.println("⚠️ Mensaje sin texto válido");
            return ResponseEntity.ok().build();
        }

        System.out.println("📨 FROM: " + msg.from());
        System.out.println("💬 BODY: " + msg.text().body());

        botService.processMessage(msg.from(), msg.text().body());

        return ResponseEntity.ok().build();
    }

    public Optional<MessageReceived> extractMessage(WebhookRequest request) {

        if (request.entry() == null || request.entry().isEmpty()) return Optional.empty();

        var entry = request.entry().get(0);

        if (entry.changes() == null || entry.changes().isEmpty()) return Optional.empty();

        var change = entry.changes().get(0);

        if (change.value() == null || change.value().messages() == null || change.value().messages().isEmpty())
            return Optional.empty();

        return Optional.of(change.value().messages().get(0));
    }
}

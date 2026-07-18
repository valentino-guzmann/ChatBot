package com.chatbotmvt.controller;

import com.chatbotmvt.dto.SendMessageDTO;
import com.chatbotmvt.services.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chats")
@CrossOrigin(origins = "https://elegant-creativity-production-5ec0.up.railway.app")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${api.key:${API_KEY:}}")
    private String apiKey;

    @GetMapping
    public ResponseEntity<?> listarChats(@RequestHeader(value = "X-API-Key", required = false) String requestKey) {
        if (!isValidApiKey(requestKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("API key requerida");
        }
        return ResponseEntity.ok(chatService.obtenerTodosLosChats());
    }

    @GetMapping("/{phone}/messages")
    public ResponseEntity<?> obtenerHistorial(@RequestHeader(value = "X-API-Key", required = false) String requestKey,
                                               @PathVariable String phone) {
        if (!isValidApiKey(requestKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("API key requerida");
        }
        return ResponseEntity.ok(chatService.obtenerHistorial(phone));
    }

    @PatchMapping("/{phone}/read")
    public ResponseEntity<?> marcarComoLeido(
            @RequestHeader(value = "X-API-Key", required = false) String requestKey,
            @PathVariable String phone) {
        if (!isValidApiKey(requestKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("API key requerida");
        }
        chatService.marcarComoLeido(phone);
        messagingTemplate.convertAndSend("/topic/updates", "read_update");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send")
    public ResponseEntity<?> enviarMensajeManual(@RequestHeader(value = "X-API-Key", required = false) String requestKey,
                                                   @RequestBody SendMessageDTO dto) {
        if (!isValidApiKey(requestKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("API key requerida");
        }
        chatService.enviarMensajeManual(dto.phone(), dto.content());
        messagingTemplate.convertAndSend("/topic/updates", "nuevo_mensaje");
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{phone}/bot-status")
    public ResponseEntity<?> cambiarBotStatus(@RequestHeader(value = "X-API-Key", required = false) String requestKey,
                                               @PathVariable String phone, @RequestBody Map<String, Boolean> body) {
        if (!isValidApiKey(requestKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("API key requerida");
        }
        chatService.actualizarEstadoBot(phone, body.get("enabled"));
        return ResponseEntity.ok().build();
    }

    private boolean isValidApiKey(String requestKey) {
        return apiKey != null && !apiKey.isBlank() && apiKey.equals(requestKey);
    }
}

package com.chatbotmvt.controller;

import com.chatbotmvt.dto.SendMessageDTO;
import com.chatbotmvt.services.ChatService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    public ResponseEntity<?> listarChats() {
        return ResponseEntity.ok(chatService.obtenerTodosLosChats());
    }

    @GetMapping("/{phone}/messages")
    public ResponseEntity<?> obtenerHistorial(@PathVariable String phone) {
        return ResponseEntity.ok(chatService.obtenerHistorial(phone));
    }

    @PostMapping("/send")
    public ResponseEntity<?> enviarMensajeManual(@RequestBody SendMessageDTO dto) {
        chatService.enviarMensajeManual(dto.phone(), dto.content());
        messagingTemplate.convertAndSend("/topic/updates", "nuevo_mensaje");
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{phone}/bot-status")
    public ResponseEntity<?> cambiarBotStatus(@PathVariable String phone, @RequestBody Map<String, Boolean> body) {
        chatService.actualizarEstadoBot(phone, body.get("enabled"));
        return ResponseEntity.ok().build();
    }
}

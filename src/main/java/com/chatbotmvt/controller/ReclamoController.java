package com.chatbotmvt.controller;

import com.chatbotmvt.entity.Reclamo;
import com.chatbotmvt.services.ChatService;
import com.chatbotmvt.services.ReclamoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reclamos")
@CrossOrigin(origins = "http://localhost:4200")
public class ReclamoController {

    private final ReclamoService reclamoService;
    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<List<Reclamo>> obtenerReclamos() {
        return ResponseEntity.ok(reclamoService.obtenerReclamos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reclamo> obtenerReclamoPorPhone(@PathVariable String phone) {
        return ResponseEntity.ok(reclamoService.obtenerPorPhone(phone));
    }

    @PatchMapping("/{phone}/estado")
    public ResponseEntity<Void> actualizarEstado(@PathVariable String phone, @RequestBody Map<String, String> body) {
        reclamoService.actualizarEstado(phone, body.get("estado"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/reclamos/{phone}/mensajes")
    public ResponseEntity<?> obtenerMensajesReclamo(@PathVariable String phone) {
        return ResponseEntity.ok(chatService.obtenerHistorial(phone));
    }
}
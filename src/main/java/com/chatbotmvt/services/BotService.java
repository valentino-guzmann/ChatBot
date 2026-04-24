package com.chatbotmvt.services;

import com.chatbotmvt.entity.TipoEstado;
import com.chatbotmvt.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotService {

    private final UsuarioService usuarioService;
    private final WhatsappService whatsappService;
    private final BotOpcionService botOpcionService;
    private final UsuarioRepository usuarioRepository;

    public void processMessage(String phone, String message) {

        log.info("📩 [BOT] Mensaje recibido -> phone: {}, message: {}", phone, message);

        var usuario = usuarioService.obtenerOCrearUsuario(phone);

        log.info("👤 [BOT] Usuario encontrado/creado -> id: {}, estado actual: {}",
                usuario.getId(),
                usuario.getCurrentState().getType());

        var estadoActual = usuario.getCurrentState();

        // Validación básica
        if (message == null || message.isBlank()) {
            log.warn("⚠️ [BOT] Mensaje vacío o null");
            whatsappService.sendMessage(phone, "No entendí tu mensaje 🤔, intentalo de nuevo");
            return;
        }

        message = message.trim();
        log.debug("✂️ [BOT] Mensaje normalizado: {}", message);

        // 👋 DETECCIÓN DE SALUDO (robusta)
        if (isGreeting(message)) {
            log.info("👋 [BOT] Saludo detectado");

            whatsappService.sendSaludoTemplate(phone);

            return; // 🔥 corta flujo
        }

        // 🧠 LÓGICA PRINCIPAL
        if (estadoActual.getType() == TipoEstado.MENU) {

            log.info("📌 [BOT] Estado MENU detectado");

            var opcion = botOpcionService.obtenerEstadoYOpcion(estadoActual, message);

            log.debug("🔎 [BOT] Opción encontrada: {}", opcion.isPresent());

            if (opcion.isPresent()) {

                var nextState = opcion.get().getNextState();

                log.info("➡️ [BOT] Cambio de estado: {} -> {}",
                        estadoActual.getType(),
                        nextState.getType());

                usuario.setCurrentState(nextState);
                usuarioRepository.save(usuario);

                log.info("💾 [BOT] Usuario actualizado en BD");

                whatsappService.sendMessage(phone, nextState.getMessage());

                log.info("📤 [BOT] Mensaje enviado: {}", nextState.getMessage());

            } else {
                log.warn("❌ [BOT] Opción inválida: {}", message);
                whatsappService.sendMessage(phone, "❌ Opción inválida, intenta nuevamente");
            }

        } else if (estadoActual.getType() == TipoEstado.INPUT) {

            log.info("⌨️ [BOT] Estado INPUT (pendiente implementación)");
            // futura lógica (ej: reclamos, formularios, etc)

        } else {
            log.error("💥 [BOT] Estado desconocido: {}", estadoActual.getType());
            whatsappService.sendMessage(phone, "⚠️ Error interno, intenta nuevamente");
        }
    }

    private boolean isGreeting(String message) {
        String msg = normalize(message);

        return msg.startsWith("hola")
                || msg.startsWith("buenas")
                || msg.startsWith("hi")
                || msg.startsWith("hello");
    }

    private String normalize(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-záéíóúñ ]", "")
                .replaceAll("(.)\\1{2,}", "$1$1")
                .trim();
    }
}
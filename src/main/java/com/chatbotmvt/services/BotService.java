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

        var usuarioOpt = usuarioRepository.findByPhone(phone);
        boolean esNuevo = usuarioOpt.isEmpty();

        var usuario = usuarioOpt.orElseGet(() -> usuarioService.obtenerOCrearUsuario(phone));

        var estadoActual = usuario.getCurrentState();

        if (estadoActual == null || estadoActual.getType() == null) {
            log.error("💥 Estado inválido en BD");
            whatsappService.sendMessage(phone, "⚠️ Error interno");
            return;
        }

        log.info("👤 Usuario -> id: {}, estado: {}", usuario.getId(), estadoActual.getType());

        // 🆕 USUARIO NUEVO → mostrar menú directamente
        if (esNuevo) {
            log.info("🆕 Usuario nuevo → enviando menú");
            whatsappService.sendMessage(phone, estadoActual.getMessage());
            return;
        }

        // Validación básica
        if (message == null || message.isBlank()) {
            whatsappService.sendMessage(phone, "No entendí tu mensaje 🤔");
            return;
        }

        message = message.trim();

        // 👋 SALUDO
        if (isGreeting(message)) {
            log.info("👋 Saludo detectado");
            whatsappService.sendSaludoTemplate(phone);
            return;
        }

        // 🧠 MENU
        if (estadoActual.getType() == TipoEstado.MENU) {

            var opcion = botOpcionService.obtenerEstadoYOpcion(estadoActual, message);

            if (opcion.isPresent()) {

                var nextState = opcion.get().getNextState();

                log.info("➡️ Cambio de estado: {} -> {}",
                        estadoActual.getType(),
                        nextState.getType());

                usuario.setCurrentState(nextState);
                usuarioRepository.save(usuario);

                whatsappService.sendMessage(phone, nextState.getMessage());

            } else {
                whatsappService.sendMessage(phone, "❌ Opción inválida, intenta nuevamente");
            }

        }
        // ✍️ INPUT
        else if (estadoActual.getType() == TipoEstado.INPUT) {

            log.info("⌨️ INPUT recibido: {}", message);

            // 👉 acá después podés guardar en DB
            whatsappService.sendMessage(phone,
                    "✅ Reclamo recibido. Lo derivamos al área correspondiente.");

            // 🔙 volver al menú principal (ID 1)
            usuario.setCurrentState(usuarioService.obtenerEstadoPorId(1L));
            usuarioRepository.save(usuario);
        }
        else {
            log.error("💥 Estado desconocido: {}", estadoActual.getType());
            whatsappService.sendMessage(phone, "⚠️ Error interno");
        }
    }

    private boolean isGreeting(String message) {
        String msg = normalize(message);

        return msg.contains("hola")
                || msg.contains("buenas")
                || msg.contains("hi")
                || msg.contains("hello");
    }

    private String normalize(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-záéíóúñ ]", "")
                .replaceAll("(.)\\1{2,}", "$1$1")
                .trim();
    }
}
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

        var estadoActual = usuario.getCurrentState();

        if (estadoActual == null || estadoActual.getType() == null) {
            log.error("💥 [BOT] Estado inválido en BD");

            whatsappService.sendMessage(phone, "⚠️ Error interno, intenta nuevamente");
            return;
        }

        log.info("👤 [BOT] Usuario -> id: {}, estado: {}",
                usuario.getId(),
                estadoActual.getType());

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

                usuario.setCurrentState(nextState);
                usuarioRepository.save(usuario);

                whatsappService.sendMessage(phone, nextState.getMessage());

            } else {
                whatsappService.sendMessage(phone, "❌ Opción inválida, intenta nuevamente");
            }

        }
        // ✍️ INPUT (tu nuevo flujo)
        else if (estadoActual.getType() == TipoEstado.INPUT) {

            log.info("⌨️ [BOT] INPUT recibido: {}", message);

            // 🔥 ACA GUARDÁS EL RECLAMO (lo dejamos simple por ahora)
            whatsappService.sendMessage(phone,
                    "✅ Reclamo recibido. Lo derivamos al área correspondiente.");

            // 👇 opcional: volver al menú
            usuario.setCurrentState(
                    usuario.getCurrentState().getId() == 16
                            ? usuarioService.obtenerEstadoPorId(1L) // MENU principal
                            : estadoActual
            );

            usuarioRepository.save(usuario);
        }
        else {
            log.error("💥 Estado desconocido: {}", estadoActual.getType());
            whatsappService.sendMessage(phone, "⚠️ Error interno");
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
package com.chatbotmvt.services;

import com.chatbotmvt.entity.TipoEstado;
import com.chatbotmvt.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BotService {

    private final UsuarioService usuarioService;
    private final WhatsappService whatsappService;
    private final BotOpcionService botOpcionService;

    private final UsuarioRepository usuarioRepository;


    public void processMessage(String phone, String message) {
        var usuario = usuarioService.obtenerOCrearUsuario(phone);

        var estadoActual = usuario.getCurrentState();

        if (message == null || message.isBlank()) {
            whatsappService.sendMessage(phone, "No entendí tu mensaje 🤔, intentalo de nuevo");
            return;
        }

        message = message.trim();

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
        } else if (estadoActual.getType() == TipoEstado.INPUT) {
            //reclamos
        } else {
            whatsappService.sendMessage(phone, "⚠️ Error interno, intenta nuevamente");
        }
    }

}
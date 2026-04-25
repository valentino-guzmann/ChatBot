package com.chatbotmvt.services;

import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.handlers.InputHandler;
import com.chatbotmvt.handlers.MenuHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotService {

    private final UsuarioSesionService usuarioSesionService;
    private final MenuHandler menuHandler;
    private final InputHandler inputHandler;
    private final BotOpcionService botOpcionService;

    public String procesarMensaje(String phone, String message) {

        log.info("📩 Mensaje recibido de [{}]: {}", phone, message);

        var usuario = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);

        var estado = usuario.getCurrentState();
        String input = message == null ? "" : message.trim();

        log.info("👤 Usuario [{}] en estado: {}", phone, estado.getName());

        if (input.equalsIgnoreCase("menu")) {
            log.info("🔄 Usuario [{}] pidió volver al menú", phone);

            var estadoInicial = usuarioSesionService.obtenerEstadoInicial();
            usuario.setCurrentState(estadoInicial);

            estado = estadoInicial;
        }

        if (estado.getType().name().equals("MENU")) {
            menuHandler.handle(usuario, input);
        } else if (estado.getType().name().equals("INPUT")) {
            inputHandler.handle(usuario, input);
        }

        usuarioSesionService.save(usuario);

        return construirRespuesta(usuario);
    }

    private String construirRespuesta(UsuarioSesion usuario) {
        var estado = usuario.getCurrentState();

        StringBuilder response = new StringBuilder();

        response.append(estado.getMessage()).append("\n\n");

        if ("error".equals(usuario.getTempData())) {
            response.append("❌ Opción inválida, intenta nuevamente\n\n");

            // 👉 limpiar error después de mostrarlo
            usuario.setTempData(null);
        }

        if (estado.getType().name().equals("MENU")) {

            var opciones = botOpcionService.obtenerOpciones(estado);

            for (var op : opciones) {
                response.append(op.getOptionKey())
                        .append("️⃣ ")
                        .append(op.getDescription())
                        .append("\n");
            }
        }

        return response.toString();
    }
}
package com.chatbotmvt.services;

import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.handlers.InputHandler;
import com.chatbotmvt.handlers.MenuHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BotService {

    private final UsuarioSesionService usuarioSesionService;
    private final MenuHandler menuHandler;
    private final InputHandler inputHandler;
    private final BotOpcionService botOpcionService;

    public String procesarMensaje(String phone, String message) {

        boolean esNuevo = false;

        var existente = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);

        if (existente.getStep() == 0 && existente.getTempData() == null) {
            esNuevo = true;
        }

        if (esNuevo) {
            return null;
        }

        var estado = existente.getCurrentState();
        String input = message == null ? "" : message.trim();

        if (estado.getType().name().equals("MENU")) {
            menuHandler.handle(existente, input);
        } else if (estado.getType().name().equals("INPUT")) {
            inputHandler.handle(existente, input);
        }

        usuarioSesionService.save(existente);

        var nuevoEstado = existente.getCurrentState();

        return construirRespuesta(nuevoEstado);
    }

    private String construirRespuesta(BotState estado) {

        StringBuilder response = new StringBuilder();

        response.append(estado.getMessage()).append("\n\n");

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
package com.chatbotmvt.services;

import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.entity.MessageLog;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.handlers.InputHandler;
import com.chatbotmvt.handlers.MenuHandler;
import com.chatbotmvt.repository.MessageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BotService {

    private final UsuarioSesionService usuarioSesionService;
    private final MenuHandler menuHandler;
    private final InputHandler inputHandler;
    private final BotOpcionService botOpcionService;
    private final MessageLogRepository messageLogRepository;

    public String procesarMensaje(String phone, String message) {
        UsuarioSesion sesion = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);

        log(phone, message, "IN", sesion.getCurrentState().getName());

        var estado = sesion.getCurrentState();

        String input = message == null ? "" : message.trim();

        if (estado.getType().name().equals("MENU")) {

            menuHandler.handle(sesion, input);

        } else if (estado.getType().name().equals("INPUT")) {

            inputHandler.handle(sesion, input);
        }

        usuarioSesionService.save(sesion);

        var nuevoEstado = sesion.getCurrentState();

        String response = construirRespuesta(nuevoEstado);
        log(phone, response, "OUT", nuevoEstado.getName());

        return response;
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

    private void log(String phone, String message, String dir, String state) {

        MessageLog log = new MessageLog();
        log.setPhone(phone);
        log.setMessage(message);
        log.setDirection(MessageLog.MessageDirection.valueOf(dir));
        log.setStateName(state);

        messageLogRepository.save(log);
    }
}
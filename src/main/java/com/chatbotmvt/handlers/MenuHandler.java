package com.chatbotmvt.handlers;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.services.BotOpcionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MenuHandler {

    private final BotOpcionService botOpcionService;

    public void handle(UsuarioSesion sesion, String message) {
        var estadoActual = sesion.getCurrentState();
        SessionData data = sesion.getTempData();

        var opcionOpt = botOpcionService.obtenerEstadoYOpcion(estadoActual, message);

        if (opcionOpt.isPresent()) {
            log.info("✅ Opción válida: [{}] seleccionada en estado [{}]", message, estadoActual.getName());

            sesion.setCurrentState(opcionOpt.get().getNextState());

            data.getExtraInfo().remove("error_menu");

        } else {
            if (message.matches("\\d+")) {
                log.warn("❌ El usuario ingresó un número [{}] que no está en el menú", message);

                data.addExtra("error_menu", "true");
            } else {
                log.info("💬 Texto libre recibido en menú (ignorando): {}", message);
            }
        }
        sesion.setTempData(data);
    }
}
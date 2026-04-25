package com.chatbotmvt.handlers;

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

        var estado = sesion.getCurrentState();

        var opcion = botOpcionService.obtenerEstadoYOpcion(estado, message);

        if (opcion.isPresent()) {

            log.info("✅ Opción válida [{}]", message);
            sesion.setCurrentState(opcion.get().getNextState());
            sesion.setTempData(null); // limpiar error

        } else {
            if (message.matches("\\d+")) {
                log.warn("❌ Opción inválida numérica: {}", message);
                sesion.setTempData("error");
            } else {
                log.info("💬 Texto libre ignorado: {}", message);
                sesion.setTempData(null);
            }
        }
    }
}
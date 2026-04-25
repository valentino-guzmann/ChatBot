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

    public void handle(UsuarioSesion sesion, String input) {

        var estado = sesion.getCurrentState();

        var opcion = botOpcionService.obtenerEstadoYOpcion(estado, input);

        if (opcion.isPresent()) {

            log.info("✅ Opción válida: {}", input);

            sesion.setCurrentState(opcion.get().getNextState());
            sesion.setStep(0);
            sesion.setTempData(null);

        } else {

            log.warn("❌ Opción inválida: {}", input);
            sesion.setTempData("error");
        }
    }
}
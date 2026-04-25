package com.chatbotmvt.handlers;

import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.services.BotOpcionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuHandler {

    private final BotOpcionService botOpcionService;

    public void handle(UsuarioSesion sesion, String message) {

        var estado = sesion.getCurrentState();

        var opcion = botOpcionService.obtenerEstadoYOpcion(estado, message);

        if (opcion.isPresent()) {
            sesion.setCurrentState(opcion.get().getNextState());
        } else {
            System.out.println("❌ Opción inválida: " + message);
        }
    }
}
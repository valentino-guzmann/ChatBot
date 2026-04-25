package com.chatbotmvt.handlers;

import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.repository.UsuarioSesionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InputHandler {

    public void handle(UsuarioSesion sesion, String message) {
        var estado = sesion.getCurrentState();

        int step = sesion.getStep() == null ? 0 : sesion.getStep();

        switch (estado.getName()) {

            case "INPUT_DESMALEZADO" -> handleDesmalezado(sesion, message, step);

            default -> System.out.println("INPUT no manejado");
        }
    }

    private void handleDesmalezado(UsuarioSesion sesion, String message, int step) {

        if (step == 0) {

            sesion.setTempData(message);
            sesion.setStep(1);

        } else if (step == 1) {

            String direccion = sesion.getTempData();
            String referencia = message;

            System.out.println("📦 Reclamo:");
            System.out.println(direccion + " / " + referencia);

            sesion.setTempData(null);
            sesion.setStep(0);

            sesion.setCurrentState(getMenuPrincipal());
        }
    }

    private BotState getMenuPrincipal() {
        BotState state = new BotState();
        state.setId(1L);
        return state;
    }
}
package com.chatbotmvt.handlers;

import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.services.BotStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class InputHandler {

    private final BotStateService botStateService;

    public void handle(UsuarioSesion sesion, String message) {

        var estado = sesion.getCurrentState();
        int step = sesion.getStep() == null ? 0 : sesion.getStep();

        switch (estado.getName()) {

            case "INPUT_DESMALEZADO" -> handleDesmalezado(sesion, message, step);

            default -> log.warn("⚠️ INPUT no manejado: {}", estado.getName());
        }
    }

    private void handleDesmalezado(UsuarioSesion sesion, String message, int step) {

        if (message == null || message.isBlank()) {
            sesion.setTempData("error");
            return;
        }

        // 📍 STEP 0 → dirección
        if (step == 0) {

            log.info("📍 Guardando dirección: {}", message);

            sesion.setTempData(message);
            sesion.setStep(1);

            sesion.setCurrentState(
                    botStateService.findByName("PEDIR_REFERENCIA")
            );

            return;
        }

        // 📌 STEP 1 → referencia
        if (step == 1) {

            String direccion = sesion.getTempData();
            String referencia = message;

            log.info("📌 Guardando referencia: {}", referencia);

            sesion.setTempData(
                    "Dirección: " + direccion + "\nReferencia: " + referencia
            );

            sesion.setStep(2);

            sesion.setCurrentState(
                    botStateService.findByName("CONFIRMACION")
            );

            return;
        }

        log.warn("⚠️ Step no manejado: {}", step);
    }
}
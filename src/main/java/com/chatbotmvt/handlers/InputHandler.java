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

        log.info("🧠 INPUT estado: {} | step: {}", estado.getName(), step);

        switch (estado.getName()) {

            case "INPUT_DESMALEZADO" -> handleDesmalezado(sesion, message, step);

            default -> log.warn("⚠️ INPUT no manejado: {}", estado.getName());
        }
    }

    private void handleDesmalezado(UsuarioSesion sesion, String message, int step) {

        if (message == null || message.isBlank()) {
            log.warn("❌ Input vacío");
            sesion.setTempData("error_input");
            return;
        }

        message = message.trim();

        if (step == 0) {

            if (message.length() < 5) {
                log.warn("❌ Dirección muy corta");
                sesion.setTempData("error_input");
                return;
            }

            log.info("📍 Guardando dirección: {}", message);

            sesion.setTempData(message);
            sesion.setStep(1);

            sesion.setCurrentState(
                    botStateService.findByName("PEDIR_REFERENCIA")
            );

            return;
        }

        if (step == 1) {

            if (message.length() < 3) {
                log.warn("❌ Referencia muy corta");
                sesion.setTempData("error_input");
                return;
            }

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
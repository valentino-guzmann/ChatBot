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
    private final ActionHandlerFactory actionHandlerFactory;

    public void handle(UsuarioSesion sesion, String message) {

        var estadoActual = sesion.getCurrentState();

        if (sesion.getTempData() == null) {
            sesion.setTempData(new SessionData());
        }

        SessionData data = sesion.getTempData();

        var opcionOpt = botOpcionService.obtenerEstadoYOpcion(estadoActual, message);

        if (opcionOpt.isPresent()) {

            var opcion = opcionOpt.get();

            log.info("✅ Opción válida: [{}] en estado [{}]", message, estadoActual.getName());

            // 🔥 1. EJECUTAR ACCIÓN SI EXISTE
            if (opcion.getActionType() != null && !opcion.getActionType().isBlank()) {

                actionHandlerFactory.getHandler(opcion.getActionType())
                        .ifPresent(handler -> handler.executeFromOption(sesion, opcion, message));
            }

            // 🔥 2. CAMBIAR ESTADO
            sesion.setCurrentState(opcion.getNextState());

            // 🔥 3. LIMPIAR ERROR
            if (data.getExtraInfo() != null) {
                data.getExtraInfo().remove("error_menu");
            }

        } else {

            if (message.matches("\\d+")) {
                log.warn("❌ Opción inválida [{}] en estado [{}]", message, estadoActual.getName());
                data.addExtra("error_menu", "true");
            } else {
                log.info("💬 Texto libre ignorado en menú: {}", message);
            }
        }

        sesion.setTempData(data);
    }
}
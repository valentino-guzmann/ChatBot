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
            log.info("✅ Opción válida: [{}]", message);

            Long idEstadoAntes = sesion.getCurrentState().getId();

            if (opcion.getActionType() != null && !opcion.getActionType().isBlank()) {
                actionHandlerFactory.getHandler(opcion.getActionType())
                        .ifPresent(handler -> handler.executeFromOption(sesion, opcion, message));
            }

            if (sesion.getCurrentState().getId().equals(idEstadoAntes)) {
                sesion.setCurrentState(opcion.getNextState());
            }

            data.getExtraInfo().remove("ignore_reply");
            data.getExtraInfo().remove("error_menu");

        } else {
            log.info("📝 Texto libre detectado en estado [{}]. El bot guardará silencio.", estadoActual.getName());
            data.addExtra("ignore_reply", "true");
        }

        sesion.setTempData(data);
    }
}
package com.chatbotmvt.handlers;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.BotOpcion;
import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.repository.BotStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ActionHandler implements BotActionHandler {
    private final BotStateRepository botStateRepository;

    @Override
    public String getActionType() { return "SET_TYPE"; }

    @Override
    public String execute(UsuarioSesion sesion, BotFlowRule rule, String input) {
        return procesarSeleccionTipo(sesion, rule.getActionValue());
    }

    @Override
    public String executeFromOption(UsuarioSesion sesion, BotOpcion opcion, String input) {
        return procesarSeleccionTipo(sesion, opcion.getActionValue());
    }

    private String procesarSeleccionTipo(UsuarioSesion sesion, String tipoInput) {
        SessionData data = sesion.getTempData();
        String tipo = tipoInput.trim().toUpperCase();
        data.setTipoReclamo(tipo);

        if (sesion.getSector() != null) {
            Long nextStateId = obtenerEstadoInputPorTipo(tipo);
            BotState nextState = botStateRepository.findById(nextStateId)
                    .orElseThrow(() -> new RuntimeException("Estado no encontrado: " + nextStateId));
            sesion.setCurrentState(nextState);
        }

        return null;
    }

    private Long obtenerEstadoInputPorTipo(String tipo) {
        return switch (tipo) {
            case "DESMALEZADO" -> 4L;
            case "BARRIDO" -> 5L;
            case "RIEGO" -> 30L;
            case "ESCOMBROS" -> 31L;
            case "BOLSONES", "DESPERDICIOS", "BOLSONES/DESPERDICIOS" -> 23L;
            default -> 21L;
        };
    }
}
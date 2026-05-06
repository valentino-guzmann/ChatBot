package com.chatbotmvt.handlers;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.BotOpcion;
import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.repository.BotStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ResetReclamoActionHandler implements BotActionHandler {
    private final BotStateRepository botStateRepository;

    @Override
    public String getActionType() { return "RESET_RECLAMO"; }

    @Override
    public String execute(UsuarioSesion sesion, BotFlowRule rule, String input) {
        return procesarReset(sesion);
    }

    @Override
    public String executeFromOption(UsuarioSesion sesion, BotOpcion opcion, String input) {
        return procesarReset(sesion);
    }

    private String procesarReset(UsuarioSesion sesion) {
        SessionData data = sesion.getTempData();
        String tipo = data.getTipoReclamo();

        Map<String, Long> mapaEstados = Map.of(
                "DESMALEZADO", 4L,
                "BARRIDO", 5L,
                "RIEGO", 30L,
                "ESCOMBROS", 31L,
                "BOLSONES", 23L,
                "DESPERDICIOS", 23L,
                "BOLSONES/DESPERDICIOS", 23L
        );

        Long nextStateId = mapaEstados.getOrDefault(tipo, 1L);
        BotState nextState = botStateRepository.findById(nextStateId)
                .orElseThrow(() -> new RuntimeException("Estado no encontrado: " + nextStateId));

        sesion.setCurrentState(nextState);

        data.setDireccion(null);
        data.setReferencia(null);

        return null;
    }
}
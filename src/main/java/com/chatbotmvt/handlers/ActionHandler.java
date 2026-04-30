package com.chatbotmvt.handlers;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.repository.BotStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActionHandler implements BotActionHandler {
    private final BotStateRepository botStateRepository;

    @Override
    public String getActionType() { return "SET_TYPE"; }

    @Override
    public String execute(UsuarioSesion sesion, BotFlowRule rule, String input) {
        SessionData data = sesion.getTempData(); // Ahora es un objeto
        String tipo = rule.getActionValue().trim().toUpperCase();

        data.setTipoReclamo(tipo);

        if (sesion.getSector() == null && requiereZona(tipo)) {
            data.addExtra("PENDIENTE", "TRUE");

            BotState elegirZona = botStateRepository.findById(14L).get();
            sesion.setCurrentState(elegirZona);

            return "📍 Para procesar este pedido necesitamos identificar tu zona primero.\n\n" + elegirZona.getMessage();
        }

        return null;
    }

    private boolean requiereZona(String tipo) {
        return "RIEGO".equals(tipo) || "ESCOMBROS".equals(tipo) ||
                "DESMALEZADO".equals(tipo) || "BARRIDO".equals(tipo);
    }
}
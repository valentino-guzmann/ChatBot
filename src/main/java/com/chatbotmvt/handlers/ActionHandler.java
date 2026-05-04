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
        SessionData data = sesion.getTempData();
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
        if (tipo == null) return false;

        return tipo.equals("RIEGO") ||
                tipo.equals("ESCOMBROS") ||
                tipo.equals("DESMALEZADO") ||
                tipo.equals("BARRIDO") ||
                tipo.equals("BOLSONES") ||
                tipo.equals("DESPERDICIOS") ||
                tipo.equals("BOLSONES/DESPERDICIOS");
    }
}
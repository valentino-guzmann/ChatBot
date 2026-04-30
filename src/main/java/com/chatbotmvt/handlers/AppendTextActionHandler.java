package com.chatbotmvt.handlers;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.UsuarioSesion;
import org.springframework.stereotype.Component;

@Component
public class AppendTextActionHandler implements BotActionHandler {
    @Override
    public String getActionType() { return "APPEND_TEXT"; }

    @Override
    public String execute(UsuarioSesion sesion, BotFlowRule rule, String input) {
        SessionData data = sesion.getTempData();

        Long stateId = sesion.getCurrentState().getId();

        if (stateId == 4L || stateId == 5L || stateId == 30L || stateId == 31L) {
            data.setDireccion(input);
        } else if (stateId == 6L) {
            data.setReferencia(input);
        } else {
            data.addExtra("LAST_INPUT", input);
        }

        return null;
    }
}

package com.chatbotmvt.handlers;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.BotOpcion;
import com.chatbotmvt.entity.UsuarioSesion;
import org.springframework.stereotype.Component;

@Component
public class SetSectorActionHandler implements BotActionHandler {

    @Override
    public String getActionType() {
        return "SET_SECTOR";
    }

    @Override
    public String execute(UsuarioSesion sesion, BotFlowRule rule, String input) {
        SessionData data = sesion.getTempData();
        Long sectorId = Long.valueOf(rule.getActionValue());
        data.setPendingSectorId(sectorId);
        return null;
    }

    @Override
    public String executeFromOption(UsuarioSesion sesion, BotOpcion opcion, String input) {
        SessionData data = sesion.getTempData();
        Long sectorId = Long.valueOf(opcion.getActionValue());
        data.setPendingSectorId(sectorId);
        return null;
    }
}
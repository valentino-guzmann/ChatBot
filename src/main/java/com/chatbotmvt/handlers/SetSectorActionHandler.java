package com.chatbotmvt.handlers;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.services.SectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SetSectorActionHandler implements BotActionHandler {

    @Override
    public String getActionType() { return "SET_SECTOR"; }

    @Override
    public String execute(UsuarioSesion sesion, BotFlowRule rule, String input) {
        SessionData data = sesion.getTempData();

        Long sectorId = Long.valueOf(rule.getActionValue());
        data.setPendingSectorId(sectorId);

        return null;
    }
}
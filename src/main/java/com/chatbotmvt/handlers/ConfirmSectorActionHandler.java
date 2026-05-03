package com.chatbotmvt.handlers;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.Sector;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.services.SectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConfirmSectorActionHandler implements BotActionHandler {
    private final SectorService sectorService;

    @Override
    public String getActionType() { return "CONFIRM_SECTOR"; }

    @Override
    public String execute(UsuarioSesion sesion, BotFlowRule rule, String input) {
        SessionData data = sesion.getTempData();

        if (data.getPendingSectorId() != null) {
            Sector sector = sectorService.findById(data.getPendingSectorId());
            sesion.setSector(sector);

            data.setPendingSectorId(null);
        }
        return null;
    }
}

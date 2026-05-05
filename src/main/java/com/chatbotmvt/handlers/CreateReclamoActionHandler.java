package com.chatbotmvt.handlers;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.repository.BotStateRepository;
import com.chatbotmvt.services.ReclamoService;
import com.chatbotmvt.services.WhatsappService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateReclamoActionHandler implements BotActionHandler {
    private final ReclamoService reclamoService;

    @Override
    public String getActionType() { return "CREATE_RECLAMO"; }

    @Override
    public String execute(UsuarioSesion sesion, BotFlowRule rule, String input) {
        SessionData data = sesion.getTempData();

        String descripcionFinal = String.format("%s. Ref: %s",
                data.getDireccion() != null ? data.getDireccion() : "",
                data.getReferencia() != null ? data.getReferencia() : ""
        );

        reclamoService.crearReclamo(
                sesion.getPhone(),
                data.getTipoReclamo(),
                descripcionFinal.trim(),
                sesion.getSector()
        );

        sesion.setTempData(new SessionData());

        return null;
    }
}
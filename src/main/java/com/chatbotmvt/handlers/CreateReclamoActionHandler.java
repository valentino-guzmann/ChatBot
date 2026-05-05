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
    private final BotStateRepository botStateRepository;
    private final WhatsappService whatsappService;

    @Override
    public String getActionType() { return "CREATE_RECLAMO"; }

    @Override
    public String execute(UsuarioSesion sesion, BotFlowRule rule, String input) {
        SessionData data = sesion.getTempData();

        System.out.println("TEMP DATA: direccion={}, referencia={}" +
                sesion.getTempData().getDireccion()+
                sesion.getTempData().getReferencia());
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

        BotState exito = botStateRepository.findById(25L).get();
        String msg = exito.getMessage();
        if (sesion.getSector() != null) {
            msg = msg.replace("{nombre}", sesion.getSector().getName());
        }
        whatsappService.sendMessage(sesion.getPhone(), msg);

        sesion.setCurrentState(botStateRepository.findById(1L).get());
        sesion.setTempData(new SessionData());

        return null;
    }
}
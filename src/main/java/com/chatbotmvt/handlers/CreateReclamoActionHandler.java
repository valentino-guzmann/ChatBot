package com.chatbotmvt.handlers;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.services.ReclamoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateReclamoActionHandler implements BotActionHandler {

    private final ReclamoService reclamoService;

    @Override
    public String getActionType() {
        return "CREATE_RECLAMO";
    }

    @Override
    public String execute(UsuarioSesion sesion, BotFlowRule rule, String input) {

        SessionData data = sesion.getTempData();

        if (data == null) {
            return "❌ Error: no hay datos del reclamo.";
        }

        if (data.getTipoReclamo() == null || data.getTipoReclamo().isBlank()) {
            return "❌ Falta el tipo de reclamo.";
        }

        if (data.getDireccion() == null || data.getDireccion().isBlank()) {
            return "❌ Falta la dirección. Volvé a ingresarla.";
        }

        if (data.getReferencia() == null || data.getReferencia().isBlank()) {
            data.setReferencia("Sin referencia");
        }

        reclamoService.crearReclamo(
                sesion.getPhone(),
                data.getTipoReclamo(),
                data,
                sesion.getSector()
        );

        // 🔥 limpiar sesión después de crear
        sesion.setTempData(new SessionData());

        return "✅ ¡Tu reclamo ha sido registrado con éxito!";
    }
}
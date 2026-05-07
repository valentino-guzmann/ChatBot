package com.chatbotmvt.handlers;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.BotOpcion;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.services.ReclamoService;
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
        return procesarCreacion(sesion);
    }

    @Override
    public String executeFromOption(UsuarioSesion sesion, BotOpcion opcion, String input) {
        return procesarCreacion(sesion);
    }

    private String procesarCreacion(UsuarioSesion sesion) {
        SessionData data = sesion.getTempData();

        String direccion = (data.getDireccion() != null) ? data.getDireccion() : "No provista";
        String referencia = (data.getReferencia() != null) ? data.getReferencia() : "Sin referencia";

        String descripcionFinal = String.format("Dirección: %s. Ref: %s", direccion, referencia);

        reclamoService.crearReclamo(
                sesion.getPhone(),
                data.getTipoReclamo(),
                descripcionFinal.trim(),
                sesion.getSector()
        );

        // Limpiamos los datos temporales
        sesion.setTempData(new SessionData());

        return null;
    }
}
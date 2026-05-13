package com.chatbotmvt.handlers;

import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.BotOpcion;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.services.ReclamoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestOperatorActionHandler implements BotActionHandler {

    private final ReclamoService reclamoService;

    @Override
    public String getActionType() {
        return "REQUEST_OPERATOR";
    }

    @Override
    public String execute(UsuarioSesion sesion, BotFlowRule rule, String input) {
        return procesarSolicitudOperador(sesion);
    }

    @Override
    public String executeFromOption(UsuarioSesion sesion, BotOpcion opcion, String input) {
        return procesarSolicitudOperador(sesion);
    }

    private String procesarSolicitudOperador(UsuarioSesion sesion) {
        log.info("🔔 ALERTA: El usuario [{}] solicitó asistencia de un operador humano.", sesion.getPhone());

        reclamoService.crearReclamo(
                sesion.getPhone(),
                "OPERADOR",
                "Solicitud de atención personalizada con un operador.",
                sesion.getSector()
        );

        // Nota: Si en el futuro agregas un modo "pausa del bot" para que no responda automáticamente
        // mientras habla el humano, puedes activarlo aquí usando tempData:
        // sesion.getTempData().addExtra("bot_paused", "true");

        return null;
    }
}
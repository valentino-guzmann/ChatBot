package com.chatbotmvt.services;

import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.handlers.MenuHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotService {

    private final UsuarioSesionService usuarioSesionService;
    private final MenuHandler menuHandler;
    private final ReclamoService reclamoService;
    private final BotFlowRuleService botFlowRuleService;

    public String procesarMensaje(String phone, String message) {

        log.info("📩 Mensaje recibido de [{}]: {}", phone, message);

        UsuarioSesion sesion = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);

        String input = message == null ? "" : message.trim();

        BotState estado = sesion.getCurrentState();

        log.info("👤 Usuario [{}] en estado: {}", phone, estado.getName());

        if (input.equalsIgnoreCase("menu") || input.equals("0")) {

            sesion.setCurrentState(usuarioSesionService.obtenerEstadoInicial());
            sesion.setTempData(null);

            usuarioSesionService.save(sesion);

            return sesion.getCurrentState().getMessage();
        }
        Optional<BotFlowRule> rule =
                botFlowRuleService.find(estado, input);

        if (rule.isPresent()) {

            BotFlowRule r = rule.get();

            log.info("⚙️ Regla aplicada: {}", r.getActionType());

            sesion.setCurrentState(r.getNextState());

            switch (r.getActionType()) {

                case "SET_TYPE":
                    sesion.setTempData(r.getActionValue() + "|");
                    break;

                case "APPEND_TEXT":
                    sesion.setTempData(
                            (sesion.getTempData() == null ? "" : sesion.getTempData())
                                    + input
                    );
                    break;

                case "CREATE_RECLAMO":

                    String[] parts = sesion.getTempData().split("\\|");

                    reclamoService.crearReclamo(
                            sesion.getPhone(),
                            parts[0],
                            parts.length > 1 ? parts[1] : ""
                    );

                    break;

                case "RESET":
                    sesion.setTempData(null);
                    sesion.setSector(null);
                    break;
            }

        } else {
            menuHandler.handle(sesion, input);
        }

        usuarioSesionService.save(sesion);

        return construirRespuesta(sesion);
    }

    private String construirRespuesta(UsuarioSesion sesion) {

        StringBuilder response = new StringBuilder();

        if (sesion.getSector() != null
                && !sesion.getCurrentState().getName().equals("CONFIRMAR_ZONA")) {

            var s = sesion.getSector();

            response.append("📅 En tu zona (")
                    .append(s.getName())
                    .append(") la recolección es el ")
                    .append(s.getSemana())
                    .append(" ")
                    .append(s.getDia())
                    .append(" del mes.\n\n");
        }

        response.append(sesion.getCurrentState().getMessage()).append("\n\n");

        if ("error".equals(sesion.getTempData())) {
            response.append("❌ Opción inválida, intenta nuevamente\n\n");
        }

        if ("error_input".equals(sesion.getTempData())) {
            response.append("❌ Ingresa un dato válido\n\n");
        }

        return response.toString();
    }
}
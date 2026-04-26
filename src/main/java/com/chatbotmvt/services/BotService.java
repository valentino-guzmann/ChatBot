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

        UsuarioSesion sesion =
                usuarioSesionService.obtenerOCrearUsuarioSesion(phone);

        String input = message == null ? "" : message.trim();

        log.info("📩 Mensaje de [{}]: {}", phone, input);

        if (input.equalsIgnoreCase("menu") || input.equals("0")) {

            log.info("🔄 Reset a menú principal");

            sesion.setCurrentState(
                    usuarioSesionService.obtenerEstadoInicial()
            );

            sesion.setTempData(null);

            usuarioSesionService.save(sesion);

            return sesion.getCurrentState().getMessage();
        }

        BotState estado = sesion.getCurrentState();

        Optional<BotFlowRule> rule =
                botFlowRuleService.find(estado, input);

        if (rule.isPresent()) {

            BotFlowRule r = rule.get();

            log.info("⚙️ FlowRule encontrada: {}", r.getInputPattern());

            sesion.setCurrentState(r.getNextState());

            switch (r.getActionType()) {

                case "SET_TYPE":
                    sesion.setTempData(r.getActionValue() + "|");
                    break;

                case "APPEND_TEXT":
                    sesion.setTempData(
                            sesion.getTempData() == null
                                    ? input
                                    : sesion.getTempData() + input
                    );
                    break;

                case "CREATE_RECLAMO":
                    String[] parts = sesion.getTempData() != null
                            ? sesion.getTempData().split("\\|")
                            : new String[]{"SIN_TIPO", "SIN_DATA"};

                    reclamoService.crearReclamo(
                            sesion.getPhone(),
                            parts[0],
                            parts.length > 1 ? parts[1] : ""
                    );
                    break;

                case "RESET":
                    sesion.setTempData(null);
                    break;
            }

        } else {
            menuHandler.handle(sesion, input);
        }

        usuarioSesionService.save(sesion);

        return sesion.getCurrentState().getMessage();
    }

    private void executeAction(BotFlowRule r,
                               UsuarioSesion sesion,
                               String message) {

        switch (r.getActionType()) {

            case "SET_TYPE":
                sesion.setTempData(r.getActionValue() + "|");
                break;

            case "APPEND_TEXT":
                sesion.setTempData(sesion.getTempData() + message);
                break;

            case "CREATE_RECLAMO":

                String[] parts = sesion.getTempData().split("\\|");

                reclamoService.crearReclamo(
                        sesion.getPhone(),
                        parts[0],
                        parts[1]
                );
                break;

            case "RESET":
                sesion.setTempData(null);
                break;
        }
    }
}
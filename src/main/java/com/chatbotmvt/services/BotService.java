package com.chatbotmvt.services;

import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.handlers.MenuHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BotService {

    private final UsuarioSesionService usuarioSesionService;
    private final MenuHandler menuHandler;
    private final ReclamoService reclamoService;
    private final BotFlowRuleService flowService;

    public String procesarMensaje(String phone, String message) {

        UsuarioSesion sesion = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);

        BotState estado = sesion.getCurrentState();

        Optional<BotFlowRule> rule =
                flowService.find(estado, message);

        if (rule.isPresent()) {

            BotFlowRule r = rule.get();

            sesion.setCurrentState(r.getNextState());

            executeAction(r, sesion, message);

        } else {

            menuHandler.handle(sesion, message);
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
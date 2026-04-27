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
    private final SectorService sectorService;

    public String procesarMensaje(String phone, String message) {

        UsuarioSesion sesion = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);
        BotState estado = sesion.getCurrentState();

        String input = message == null ? "" : message.trim();

        if (input.equalsIgnoreCase("menu") || input.equals("0")) {
            sesion.setCurrentState(usuarioSesionService.obtenerEstadoInicial());
            sesion.setTempData(null);
            sesion.setSector(null);

            usuarioSesionService.save(sesion);
            return sesion.getCurrentState().getMessage();
        }

        String customResponse = null;

        Optional<BotFlowRule> rule = botFlowRuleService.find(estado, input);

        if (rule.isPresent()) {

            BotFlowRule r = rule.get();

            sesion.setCurrentState(r.getNextState());

            switch (r.getActionType()) {

                case "SET_TYPE":
                    sesion.setTempData(r.getActionValue() + "|");
                    break;

                case "APPEND_TEXT":
                    sesion.setTempData(
                            (sesion.getTempData() == null ? "" : sesion.getTempData())
                                    + input + " "
                    );
                    break;

                case "SET_SECTOR":
                    Long sectorId = Long.valueOf(input);

                    sesion.setTempData("SECTOR|" + sectorId);

                    customResponse = sectorService.construirMensajeZona(sectorId);
                    break;

                case "CONFIRM_SECTOR":
                    String temp = sesion.getTempData();

                    if (temp != null && temp.startsWith("SECTOR|")) {

                        Long sectorIdConfirmado =
                                Long.valueOf(temp.split("\\|")[1]);

                        var sectorConfirmado =
                                sectorService.findById(sectorIdConfirmado);

                        sesion.setSector(sectorConfirmado);
                    }

                    sesion.setTempData(null);
                    break;

                case "RESET_SECTOR":
                    sesion.setSector(null);
                    sesion.setTempData(null);
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
                    break;
            }

        } else {
            menuHandler.handle(sesion, input);
        }

        usuarioSesionService.save(sesion);

        return customResponse != null
                ? customResponse
                : sesion.getCurrentState().getMessage();
    }
}
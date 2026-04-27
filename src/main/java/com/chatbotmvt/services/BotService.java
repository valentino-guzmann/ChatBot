package com.chatbotmvt.services;

import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.entity.Sector;
import com.chatbotmvt.handlers.MenuHandler;
import com.chatbotmvt.repository.BotStateRepository;
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
    private final WhatsappService whatsappService;
    private final BotStateRepository botStateRepository;

    public String procesarMensaje(String phone, String message) {

        UsuarioSesion sesion = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);
        BotState estado = sesion.getCurrentState();

        String input = message == null ? "" : message.trim();

        if (input.equalsIgnoreCase("menu") || input.equals("0")) {
            sesion.setCurrentState(usuarioSesionService.obtenerEstadoInicial());
            sesion.setTempData(null);
            usuarioSesionService.save(sesion);
            return sesion.getCurrentState().getMessage();
        }

        String customResponse = null;

        Optional<BotFlowRule> rule = botFlowRuleService.find(estado, input);

        if (rule.isPresent()) {
            BotFlowRule r = rule.get();
            sesion.setCurrentState(r.getNextState());

            log.info("⚙️ Ejecutando Acción: {} para el input: {}", r.getActionType(), input);

            switch (r.getActionType()) {
                case "SET_TYPE":
                    sesion.setTempData(r.getActionValue() + "|");
                    break;

                case "APPEND_TEXT":
                    sesion.setTempData((sesion.getTempData() == null ? "" : sesion.getTempData()) + input + " ");
                    break;

                case "SET_SECTOR":
                    Long sectorId = Long.valueOf(r.getActionValue());
                    sesion.setTempData("SECTOR|" + sectorId);
                    break;

                case "CONFIRM_SECTOR":
                    String temp = sesion.getTempData();
                    if (temp != null && temp.startsWith("SECTOR|")) {
                        Long sectorIdConfirmado = Long.parseLong(temp.split("\\|")[1]);
                        Sector sector = sectorService.findById(sectorIdConfirmado);

                        sesion.setSector(sector); // Guardado permanente en la sesión

                        if (sector.getImageUrl() != null && !sector.getImageUrl().isEmpty()) {
                            whatsappService.sendImage(sesion.getPhone(), sector.getImageUrl());
                        }
                    }
                    sesion.setTempData(null);
                    break;

                case "RESET_SECTOR":
                    sesion.setSector(null);
                    sesion.setTempData(null);
                    break;

                case "CREATE_RECLAMO":
                    if (sesion.getTempData() != null) {
                        String[] parts = sesion.getTempData().split("\\|");
                        reclamoService.crearReclamo(
                                sesion.getPhone(),
                                parts[0],
                                parts.length > 1 ? parts[1] : "",
                                sesion.getSector()
                        );
                    }
                    break;

                case "RESET":
                    sesion.setTempData(null);
                    break;
            }
        } else {
            menuHandler.handle(sesion, input);
        }

        if (sesion.getSector() != null && (sesion.getCurrentState().getId() == 8 || sesion.getCurrentState().getId() == 9)) {
            BotState estadoExito = botStateRepository.findById(21L)
                    .orElseThrow(() -> new RuntimeException("No existe estado de éxito 21"));
            sesion.setCurrentState(estadoExito);
        }

        usuarioSesionService.save(sesion);

        String finalResponse = customResponse != null
                ? customResponse
                : sesion.getCurrentState().getMessage();

        if (sesion.getSector() != null) {
            String nombre = sesion.getSector().getName() != null ? sesion.getSector().getName() : "";
            String link = sesion.getSector().getCalendarLink() != null ? sesion.getSector().getCalendarLink() : "";

            finalResponse = finalResponse
                    .replace("{nombre}", nombre)
                    .replace("{link}", link);
        }

        return finalResponse;
    }
}
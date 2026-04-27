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

        Optional<BotFlowRule> rule = botFlowRuleService.find(estado, input);

        if (rule.isPresent()) {
            BotFlowRule r = rule.get();
            sesion.setCurrentState(r.getNextState());

            log.info("⚙️ Ejecutando Acción: {} | Estado: {} | Input: {}", r.getActionType(), estado.getId(), input);

            switch (r.getActionType()) {
                case "SET_TYPE":
                    String tipo = r.getActionValue();
                    // VALIDACIÓN: Si pide Riego/Escombros y no tiene zona, lo mandamos a elegirla
                    if (sesion.getSector() == null && (tipo.equals("RIEGO") || tipo.equals("ESCOMBROS"))) {
                        sesion.setTempData("PENDIENTE_" + tipo + "|");
                        BotState elegirZona = botStateRepository.findById(14L).get();
                        sesion.setCurrentState(elegirZona);

                        usuarioSesionService.save(sesion);
                        return "📍 Para procesar este pedido necesitamos identificar tu zona primero.\n\n" + elegirZona.getMessage();
                    }
                    sesion.setTempData(tipo + "|");
                    break;

                case "APPEND_TEXT":
                    String dataActual = sesion.getTempData() == null ? "" : sesion.getTempData();
                    sesion.setTempData(dataActual + input + "|");
                    break;

                case "SET_SECTOR":
                    Long sectorId = Long.valueOf(r.getActionValue());
                    String dataPrevia = sesion.getTempData() != null ? sesion.getTempData() : "";
                    sesion.setTempData(dataPrevia + "ZONA:" + sectorId + "|");
                    break;

                case "CONFIRM_SECTOR":
                    String temp = sesion.getTempData();
                    if (temp != null && temp.contains("ZONA:")) {
                        String idStr = temp.substring(temp.indexOf("ZONA:") + 5).split("\\|")[0];
                        Sector sector = sectorService.findById(Long.parseLong(idStr));
                        sesion.setSector(sector);

                        if (temp.contains("PENDIENTE_RIEGO")) {
                            sesion.setCurrentState(botStateRepository.findById(30L).get());
                            sesion.setTempData("RIEGO|");
                        } else if (temp.contains("PENDIENTE_ESCOMBROS")) {
                            sesion.setCurrentState(botStateRepository.findById(31L).get());
                            sesion.setTempData("ESCOMBROS|");
                        } else {
                            sesion.setCurrentState(r.getNextState());
                            sesion.setTempData(null);
                        }

                        if (sector.getImageUrl() != null && !sector.getImageUrl().isEmpty()) {
                            whatsappService.sendImage(sesion.getPhone(), sector.getImageUrl());
                        }
                    }
                    break;

                case "CREATE_RECLAMO":
                    if (sesion.getTempData() != null) {
                        String[] parts = sesion.getTempData().split("\\|");
                        reclamoService.crearReclamo(
                                sesion.getPhone(),
                                parts[0],
                                (parts.length > 1 ? parts[1] : "") + " " + (parts.length > 2 ? parts[2] : ""),
                                sesion.getSector()
                        );
                    }
                    sesion.setTempData(null);
                    break;

                case "RESET":
                    if (sesion.getTempData() != null) {
                        String tipoActual = sesion.getTempData().split("\\|")[0];
                        Long nextId = switch (tipoActual) {
                            case "BARRIDO" -> 5L;
                            case "RIEGO" -> 30L;
                            case "ESCOMBROS" -> 31L;
                            case "BOLSONES/DESPERDICIOS" -> 23L;
                            default -> 4L;
                        };
                        sesion.setCurrentState(botStateRepository.findById(nextId).get());
                        sesion.setTempData(tipoActual + "|");
                    } else {
                        sesion.setTempData(null);
                    }
                    break;

                case "RESET_SECTOR":
                    sesion.setSector(null);
                    sesion.setTempData(null);
                    break;
            }
        } else {
            menuHandler.handle(sesion, input);
        }

        if (sesion.getSector() != null && sesion.getCurrentState() != null) {
            Long idEstadoActual = sesion.getCurrentState().getId();
            if (Long.valueOf(8).equals(idEstadoActual) || Long.valueOf(9).equals(idEstadoActual)) {
                sesion.setCurrentState(botStateRepository.findById(21L).get());
            }
        }

        usuarioSesionService.save(sesion);

        String finalResponse = sesion.getCurrentState().getMessage();

        if (sesion.getSector() != null) {
            String nombre = sesion.getSector().getName() != null ? sesion.getSector().getName() : "tu zona";
            String link = sesion.getSector().getCalendarLink() != null ? sesion.getSector().getCalendarLink() : "";

            finalResponse = finalResponse
                    .replace("{nombre}", nombre)
                    .replace("{link}", link);
        }

        return finalResponse;
    }
}
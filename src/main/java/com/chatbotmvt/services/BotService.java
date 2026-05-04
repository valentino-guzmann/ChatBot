package com.chatbotmvt.services;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.entity.Sector;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.handlers.ActionHandlerFactory;
import com.chatbotmvt.handlers.MenuHandler;
import com.chatbotmvt.repository.BotStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotService {

    private final UsuarioSesionService usuarioSesionService;
    private final MenuHandler menuHandler;
    private final BotFlowRuleService botFlowRuleService;
    private final ActionHandlerFactory actionHandlerFactory;
    private final BotStateRepository botStateRepository;
    private final WhatsappService whatsappService;
    private final SectorService sectorService;

    @Transactional
    public String procesarMensaje(String phone, String message) {
        UsuarioSesion sesion = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);

        if (sesion.getTempData() != null) {
            sesion.getTempData().getExtraInfo().remove("error_menu");
        }

        BotState estadoActual = sesion.getCurrentState();
        String input = (message == null) ? "" : message.trim();

        log.debug("Procesando mensaje para [{}]. Estado actual: [{}]. Input: [{}]",
                phone, estadoActual.getName(), input);

        if (input.equalsIgnoreCase("menu") || input.equals("0")) {
            return resetearAlMenuInicial(sesion);
        }

        Optional<BotFlowRule> ruleOpt = botFlowRuleService.find(estadoActual, input);

        if (ruleOpt.isPresent()) {
            BotFlowRule rule = ruleOpt.get();

            sesion.setCurrentState(rule.getNextState());

            String customResponse = actionHandlerFactory.getHandler(rule.getActionType())
                    .map(handler -> handler.execute(sesion, rule, input))
                    .orElse(null);

            if (customResponse != null) {
                usuarioSesionService.save(sesion);
                return reemplazarEtiquetas(customResponse, sesion);
            }

        } else {
            menuHandler.handle(sesion, input);
        }

        verificarTransicionesEspeciales(sesion);

        usuarioSesionService.save(sesion);

        return reemplazarEtiquetas(sesion.getCurrentState().getMessage(), sesion);
    }

    @Async
    public void procesarYResponder(String phone, String text) {
        try {
            String respuestaTexto = procesarMensaje(phone, text);
            UsuarioSesion sesion = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);
            BotState estadoActual = sesion.getCurrentState();

            if (estadoActual.getTemplateName() != null) {
                log.debug("Enviando template prioritario: {}", estadoActual.getTemplateName());
                whatsappService.sendTemplate(
                        phone,
                        estadoActual.getTemplateName(),
                        estadoActual.getImageUrl()
                );
                Thread.sleep(500);
            }

            if (respuestaTexto != null && !respuestaTexto.isBlank()) {
                whatsappService.sendMessage(phone, respuestaTexto);
            }

        } catch (Exception e) {
            log.error("Error en procesarYResponder: {}", e.getMessage());
        }
    }

    private String resetearAlMenuInicial(UsuarioSesion sesion) {
        BotState estadoInicial = usuarioSesionService.obtenerEstadoInicial();
        sesion.setCurrentState(estadoInicial);
        sesion.setTempData(new SessionData());
        usuarioSesionService.save(sesion);
        return reemplazarEtiquetas(estadoInicial.getMessage(), sesion);
    }

    private String reemplazarEtiquetas(String mensaje, UsuarioSesion sesion) {
        if (mensaje == null) return "";
        String resultado = mensaje;
        SessionData data = sesion.getTempData();

        String nombreParaReemplazar = null;
        String linkParaReemplazar = "";

        if (sesion.getSector() != null) {
            nombreParaReemplazar = sesion.getSector().getName();
            linkParaReemplazar = sesion.getSector().getCalendarLink();
        } else if (data != null && data.getPendingSectorId() != null) {
            try {
                Sector sectorPendiente = sectorService.findById(data.getPendingSectorId());
                nombreParaReemplazar = sectorPendiente.getName();
                linkParaReemplazar = sectorPendiente.getCalendarLink();
            } catch (Exception e) {
                log.warn("No se pudo precargar el sector pendiente para las etiquetas");
            }
        }

        if (nombreParaReemplazar != null) {
            resultado = resultado.replace("{nombre}", nombreParaReemplazar);
        } else {
            resultado = resultado.replace("{nombre}", "tu zona");
        }

        if (linkParaReemplazar != null) {
            resultado = resultado.replace("{link}", linkParaReemplazar);
        }

        if (data != null && "true".equals(data.getExtraInfo().get("error_menu"))) {
            resultado = "⚠️ *Opción no válida.*\n" + resultado;
        }

        return resultado;
    }

    private void verificarTransicionesEspeciales(UsuarioSesion sesion) {
        if (sesion.getSector() != null && sesion.getCurrentState() != null) {
            Long stateId = sesion.getCurrentState().getId();

            if (Long.valueOf(8).equals(stateId) || Long.valueOf(9).equals(stateId)) {
                botStateRepository.findById(21L).ifPresent(sesion::setCurrentState);
            }
        }
    }
}
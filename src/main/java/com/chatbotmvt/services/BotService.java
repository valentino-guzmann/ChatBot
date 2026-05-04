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

    @Async("botExecutor")
    public void procesarYResponder(String phone, String text) {
        try {
            log.info("Iniciando procesamiento asíncrono para: {}", phone);

            RespuestaBot resultado = ejecutarLogicaYGuardar(phone, text);

            if (resultado.templateName() != null) {
                log.debug("Enviando template: {}", resultado.templateName());
                whatsappService.sendTemplate(phone, resultado.templateName(), resultado.mediaId());
                Thread.sleep(400);
            }

            if (resultado.mensajeTexto() != null && !resultado.mensajeTexto().isBlank()) {
                whatsappService.sendMessage(phone, resultado.mensajeTexto());
            }

        } catch (Exception e) {
            log.error("Error crítico en BotService: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public RespuestaBot ejecutarLogicaYGuardar(String phone, String text) {
        UsuarioSesion sesion = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);

        if (sesion.getTempData() == null) {
            sesion.setTempData(new SessionData());
        }

        sesion.getTempData().getExtraInfo().remove("error_menu");

        String mensajeParaEnviar = procesarFlujoInterno(sesion, text);

        usuarioSesionService.save(sesion);

        return new RespuestaBot(
                mensajeParaEnviar,
                sesion.getCurrentState().getTemplateName(),
                sesion.getCurrentState().getMediaId()
        );
    }

    private String procesarFlujoInterno(UsuarioSesion sesion, String message) {
        BotState estadoActual = sesion.getCurrentState();
        String input = (message == null) ? "" : message.trim();

        log.debug("Procesando [{}]. Estado: [{}]. Input: [{}]", sesion.getPhone(), estadoActual.getName(), input);

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
                return reemplazarEtiquetas(customResponse, sesion);
            }

        } else {
            menuHandler.handle(sesion, input);
        }

        verificarTransicionesEspeciales(sesion);

        return reemplazarEtiquetas(sesion.getCurrentState().getMessage(), sesion);
    }

    private String resetearAlMenuInicial(UsuarioSesion sesion) {
        BotState estadoInicial = usuarioSesionService.obtenerEstadoInicial();
        sesion.setCurrentState(estadoInicial);
        sesion.setTempData(new SessionData());
        return reemplazarEtiquetas(estadoInicial.getMessage(), sesion);
    }

    private String reemplazarEtiquetas(String mensaje, UsuarioSesion sesion) {
        if (mensaje == null) return "";
        String resultado = mensaje;
        SessionData data = sesion.getTempData();

        String nombreSector = "tu zona";
        String linkSector = "";

        if (sesion.getSector() != null) {
            nombreSector = sesion.getSector().getName();
            linkSector = sesion.getSector().getCalendarLink();
        } else if (data != null && data.getPendingSectorId() != null) {
            try {
                Sector sectorPendiente = sectorService.findById(data.getPendingSectorId());
                nombreSector = sectorPendiente.getName();
                linkSector = sectorPendiente.getCalendarLink();
            } catch (Exception e) {
                log.warn("No se pudo precargar sector pendiente para etiquetas");
            }
        }

        resultado = resultado.replace("{nombre}", nombreSector);
        resultado = resultado.replace("{link}", linkSector != null ? linkSector : "");

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

    private record RespuestaBot(String mensajeTexto, String templateName, String mediaId) {}
}
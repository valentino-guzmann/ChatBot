package com.chatbotmvt.services;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.entity.Sector;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.handlers.ActionHandlerFactory;
import com.chatbotmvt.handlers.MenuHandler;
import com.chatbotmvt.repository.BotStateRepository;
import com.github.benmanes.caffeine.cache.Cache;
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

    private final Cache<Long, BotState> botStateCache;
    private final Cache<Long, Sector> sectorCache;

    @Async("botExecutor")
    public void procesarYResponder(String phone, String text) {

        try {
            log.info("Iniciando procesamiento asíncrono para: {}", phone);

            UsuarioSesion sesion = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);

            RespuestaBot resultado = ejecutarLogicaYGuardar(sesion, text);

            if (resultado.templateName() != null && !resultado.templateName().isBlank()) {

                whatsappService.sendTemplate(
                        phone,
                        resultado.templateName(),
                        resultado.mediaId()
                );

            } else if (resultado.mensajeTexto() != null && !resultado.mensajeTexto().isBlank()) {

                whatsappService.sendMessage(phone, resultado.mensajeTexto());
            }

        } catch (Exception e) {
            log.error("Error crítico en BotService: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public RespuestaBot ejecutarLogicaYGuardar(UsuarioSesion sesion, String text) {
        if (sesion.getTempData() == null) {
            sesion.setTempData(new SessionData());
        }

        SessionData data = sesion.getTempData();
        data.getExtraInfo().remove("error_menu");

        Long stateBefore = sesion.getCurrentState().getId();
        String mensajeParaEnviar = procesarFlujoInterno(sesion, text);
        Long stateAfter = sesion.getCurrentState().getId();

        String templateToHead = sesion.getCurrentState().getTemplateName();
        String mediaId = sesion.getCurrentState().getMediaId();

        if (templateToHead != null) {
            String key = "media_sent_" + stateAfter;
            if ("true".equals(data.getExtraInfo().get(key))) {
                templateToHead = null;
                mediaId = null;
            } else {
                data.addExtra(key, "true");
            }
        }

        usuarioSesionService.save(sesion);

        return new RespuestaBot(
                mensajeParaEnviar,
                templateToHead,
                mediaId
        );
    }

    private String procesarFlujoInterno(UsuarioSesion sesion, String message) {
        BotState estadoOrigen = sesion.getCurrentState();
        String input = (message == null) ? "" : message.trim();

        log.debug("Procesando [{}]. Estado Origen: [{}]. Input: [{}]",
                sesion.getPhone(), estadoOrigen.getName(), input);

        if (input.equalsIgnoreCase("menu") || input.equals("0")) {
            return resetearAlMenuInicial(sesion);
        }

        Optional<BotFlowRule> ruleOpt = botFlowRuleService.find(estadoOrigen, input);

        if (ruleOpt.isPresent()) {
            BotFlowRule rule = ruleOpt.get();

            String customResponse = actionHandlerFactory.getHandler(rule.getActionType())
                    .map(handler -> handler.execute(sesion, rule, input))
                    .orElse(null);

            BotState nextState = botStateCache.get(
                    rule.getNextState().getId(),
                    id -> botStateRepository.findById(id).orElse(null)
            );

            if (nextState != null) {
                sesion.setCurrentState(nextState);
                log.debug("Transición de estado: [{}] -> [{}]", estadoOrigen.getName(), nextState.getName());
            }

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

        BotState cached = botStateCache.get(
                estadoInicial.getId(),
                id -> botStateRepository.findById(id).orElse(estadoInicial)
        );

        sesion.setCurrentState(cached);
        sesion.setTempData(new SessionData());

        return reemplazarEtiquetas(cached.getMessage(), sesion);
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
                Sector sectorPendiente = sectorCache.get(
                        data.getPendingSectorId(),
                        id -> sectorService.findById(id)
                );

                nombreSector = sectorPendiente.getName();
                linkSector = sectorPendiente.getCalendarLink();

            } catch (Exception e) {
                log.warn("No se pudo precargar sector pendiente");
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

                BotState state21 = botStateCache.get(
                        21L,
                        id -> botStateRepository.findById(id).orElse(null)
                );

                if (state21 != null) {
                    sesion.setCurrentState(state21);
                }
            }
        }
    }

    private record RespuestaBot(String mensajeTexto, String templateName, String mediaId) {}
}
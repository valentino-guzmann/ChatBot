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
            UsuarioSesion sesion = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);
            RespuestaBot resultado = ejecutarLogicaYGuardar(sesion, text);

            if (resultado.templateName() != null) {
                whatsappService.sendTemplate(phone, resultado.templateName(), resultado.mediaId());
            }

            if (resultado.mensajeTexto() != null && !resultado.mensajeTexto().isBlank()) {
                whatsappService.sendMessage(phone, resultado.mensajeTexto());
            }

        } catch (Exception e) {
            log.error("Error en BotService: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public RespuestaBot ejecutarLogicaYGuardar(UsuarioSesion sesion, String text) {
        if (sesion.getTempData() == null) {
            sesion.setTempData(new SessionData());
        }

        String mensajeTexto = procesarFlujoInterno(sesion, text);

        usuarioSesionService.save(sesion);

        String mensajeFinal = reemplazarEtiquetas(mensajeTexto, sesion);

        BotState estadoNuevo = sesion.getCurrentState();

        String templateToSend = estadoNuevo.getTemplateName();
        String mediaIdToSend = estadoNuevo.getMediaId();

        return new RespuestaBot(mensajeFinal, templateToSend, mediaIdToSend);
    }

    private String procesarFlujoInterno(UsuarioSesion sesion, String message) {
        BotState estadoOrigen = sesion.getCurrentState();
        String input = (message == null) ? "" : message.trim().toLowerCase();

        log.info("STATE [{}] INPUT [{}]", estadoOrigen.getId(), input);

        if (input.equals("menu") || input.equals("0")) {
            return resetearAlMenuInicial(sesion);
        }

        // 🔥 1. BUSCAR RULE EXACTA
        Optional<BotFlowRule> ruleOpt = botFlowRuleService.findExact(estadoOrigen, input);

        // 🔥 2. SI NO HAY → DEFAULT
        if (ruleOpt.isEmpty()) {
            ruleOpt = botFlowRuleService.findDefault(estadoOrigen);
        }

        if (ruleOpt.isPresent()) {
            BotFlowRule rule = ruleOpt.get();

            log.info("RULE MATCH → {}", rule.getId());

            actionHandlerFactory.getHandler(rule.getActionType())
                    .ifPresent(handler -> handler.execute(sesion, rule, input));

            if (rule.getNextState() != null) {
                BotState nextState = botStateCache.get(
                        rule.getNextState().getId(),
                        id -> botStateRepository.findById(id).orElse(null)
                );

                if (nextState != null) {
                    sesion.setCurrentState(nextState);
                }
            }

        } else {
            menuHandler.handle(sesion, input);
        }

        return sesion.getCurrentState().getMessage();
    }

    private String resetearAlMenuInicial(UsuarioSesion sesion) {
        BotState estadoInicial = usuarioSesionService.obtenerEstadoInicial();

        BotState cached = botStateCache.get(
                estadoInicial.getId(),
                id -> botStateRepository.findById(id).orElse(estadoInicial)
        );

        sesion.setCurrentState(cached);
        sesion.setTempData(new SessionData());

        return cached.getMessage();
    }

    private String reemplazarEtiquetas(String mensaje, UsuarioSesion sesion) {

        if (mensaje == null) return "";

        SessionData data = sesion.getTempData();

        String nombreSector = "tu zona";
        String linkSector = "";

        if (sesion.getSector() != null) {
            nombreSector = sesion.getSector().getName();
            linkSector = sesion.getSector().getCalendarLink();
        }

        mensaje = mensaje.replace("{nombre}", nombreSector);
        mensaje = mensaje.replace("{link}", linkSector != null ? linkSector : "");

        if (data != null && "true".equals(data.getExtraInfo().get("error_menu"))) {
            mensaje = "⚠️ Opción no válida\n" + mensaje;
        }

        return mensaje;
    }

    private record RespuestaBot(String mensajeTexto, String templateName, String mediaId) {}
}
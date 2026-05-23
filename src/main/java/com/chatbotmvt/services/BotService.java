package com.chatbotmvt.services;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.entity.*;
import com.chatbotmvt.handlers.ActionHandlerFactory;
import com.chatbotmvt.handlers.MenuHandler;
import com.chatbotmvt.repository.BotStateRepository;
import com.chatbotmvt.repository.MensajeLogRepository; // Nuevo
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate; // Nuevo
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final MensajeLogRepository mensajeLogRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final Cache<Long, BotState> botStateCache;

    @Async("botExecutor")
    public void procesarYResponder(String phone, String text) {
        try {
            UsuarioSesion sesion = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);

            registrarMensaje(phone, text, "USER");
            messagingTemplate.convertAndSend("/topic/updates", phone);

            if (sesion.getBotEnabled() != null && !sesion.getBotEnabled()) {
                log.info("🤖 Bot pausado para {}. Esperando respuesta humana.", phone);
                return;
            }

            RespuestaBot resultado = ejecutarLogicaYGuardar(sesion, text);

            SessionData data = sesion.getTempData();
            if (data != null && data.getExtraInfo().containsKey("ignore_reply")) {
                log.info("🤫 Opción inválida. Bot silenciado para {}", phone);
                data.getExtraInfo().remove("ignore_reply");
                usuarioSesionService.save(sesion);
                return;
            }

            if (resultado != null && resultado.mensajeTexto() != null && !resultado.mensajeTexto().isBlank()) {
                BotState estadoActual = sesion.getCurrentState(); // Necesitamos el estado para la imagen

                if (estadoActual.getTemplateName() != null && !estadoActual.getTemplateName().isBlank()) {
                    // Enviar imagen con texto
                    whatsappService.sendImage(phone, estadoActual.getTemplateName(), resultado.mensajeTexto());
                } else if (resultado.templateName() != null) {
                    whatsappService.sendTemplate(phone, resultado.templateName(), resultado.mediaId(), resultado.mensajeTexto());
                } else {
                    whatsappService.sendMessage(phone, resultado.mensajeTexto());
                }

                registrarMensaje(phone, resultado.mensajeTexto(), "BOT");
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

        Optional<BotFlowRule> ruleOpt = botFlowRuleService.findExact(estadoOrigen, input);

        if (ruleOpt.isEmpty()) {
            ruleOpt = botFlowRuleService.findDefault(estadoOrigen);
        }

        if (ruleOpt.isPresent()) {
            BotFlowRule rule = ruleOpt.get();
            log.info("RULE MATCH → {}", rule.getId());

            Long idAntes = sesion.getCurrentState().getId();

            actionHandlerFactory.getHandler(rule.getActionType())
                    .ifPresent(handler -> handler.execute(sesion, rule, input));

            if (rule.getNextState() != null && sesion.getCurrentState().getId().equals(idAntes)) {
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

    // --- NUEVOS MÉTODOS PARA DASHBOARD Y OPERADOR ---

    public void enviarMensajeManual(String phone, String content) {
        // Enviar a WhatsApp
        whatsappService.sendMessage(phone, content);

        // Registrar mensaje como Operador
        registrarMensaje(phone, content, "OPERATOR");

        // Notificar al dashboard para que vea su propio mensaje
        messagingTemplate.convertAndSend("/topic/updates", phone);
    }

    @Transactional
    public void actualizarEstadoBot(String phone, boolean enabled) {
        UsuarioSesion sesion = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);
        sesion.setBotEnabled(enabled);
        usuarioSesionService.save(sesion);

        // Notificar al dashboard que el status del bot cambió
        messagingTemplate.convertAndSend("/topic/updates", phone);
    }

    private void registrarMensaje(String phone, String content, String sender) {
        MensajeLog logEntry = new MensajeLog();
        logEntry.setPhone(phone);
        logEntry.setContent(content);
        logEntry.setSender(sender);
        logEntry.setCreatedAt(LocalDateTime.now());
        mensajeLogRepository.save(logEntry);
    }

    // --- MÉTODOS DE APOYO ORIGINALES ---

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

        return mensaje;
    }

    private record RespuestaBot(String mensajeTexto, String templateName, String mediaId) {}
}
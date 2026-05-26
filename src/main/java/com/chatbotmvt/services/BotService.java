package com.chatbotmvt.services;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.entity.*;
import com.chatbotmvt.handlers.ActionHandlerFactory;
import com.chatbotmvt.handlers.MenuHandler;
import com.chatbotmvt.repository.BotStateRepository;
import com.chatbotmvt.repository.MensajeLogRepository;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
                log.info("🤖 Bot pausado para {}.", phone);
                return;
            }

            RespuestaBot resultado = ejecutarLogicaYGuardar(sesion, text);

            // --- LÓGICA DE SILENCIO (Restricción 24hs) ---
            SessionData data = sesion.getTempData();
            if (data != null && data.getExtraInfo().containsKey("ignore_reply")) {
                log.info("🤫 Silencio aplicado para {}. No se envía respuesta.", phone);
                data.getExtraInfo().remove("ignore_reply");
                usuarioSesionService.save(sesion);
                return;
            }

            if (resultado != null && resultado.mensajeTexto() != null) {
                BotState estadoActual = sesion.getCurrentState();

                // 1. Prioridad: Plantillas de Meta (Para menús de selección con imagen)
                if (estadoActual.getTemplateName() != null && !estadoActual.getTemplateName().isBlank()) {
                    // Pasamos null en el texto para que use el texto fijo configurado en Meta y no el de la DB
                    whatsappService.sendTemplate(phone, estadoActual.getTemplateName(), estadoActual.getMediaId(), null);
                }
                // 2. Prioridad: Imagen con Media ID (Para barrios/zonas específicas)
                else if (estadoActual.getMediaId() != null && !estadoActual.getMediaId().isBlank()) {
                    // Enviamos el texto de la DB (barrios) como pie de foto (caption)
                    whatsappService.sendImageById(phone, estadoActual.getMediaId(), resultado.mensajeTexto());
                }
                // 3. Prioridad: Mensaje de texto simple
                else {
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

        return new RespuestaBot(mensajeFinal, estadoNuevo.getTemplateName(), estadoNuevo.getMediaId());
    }

    private String procesarFlujoInterno(UsuarioSesion sesion, String message) {
        BotState estadoOrigen = sesion.getCurrentState();
        String input = (message == null) ? "" : message.trim().toLowerCase();
        SessionData data = sesion.getTempData();
        LocalDateTime now = LocalDateTime.now();

        // --- A. RESTRICCIÓN EXPLÍCITA (0 o MENU) ---
        if (input.equals("menu") || input.equals("0")) {
            if (estaEnPeriodoDeBloqueo(data, now) && estadoOrigen.getId() != 1L) {
                log.info("🚫 Intento de reset bloqueado por 24hs.");
                data.addExtra("ignore_reply", "true");
                return null;
            }
            actualizarTimestampMenu(data, now);
            return resetearAlMenuInicial(sesion);
        }

        if (estadoOrigen.getId() == 1L) {
            Optional<BotOpcion> opt = botOpcionService_obtenerOpcion(estadoOrigen, input);
            if (opt.isPresent()) {
                actualizarTimestampMenu(data, now);
                menuHandler.handle(sesion, input);
                return sesion.getCurrentState().getMessage();
            }

            if (estaEnPeriodoDeBloqueo(data, now)) {
                log.info("🤫 Silenciando mensaje en menú por regla de 24hs.");
                data.addExtra("ignore_reply", "true");
                return null;
            } else {
                actualizarTimestampMenu(data, now);
                return estadoOrigen.getMessage();
            }
        }

        Optional<BotFlowRule> ruleOpt = botFlowRuleService.findExact(estadoOrigen, input);
        if (ruleOpt.isEmpty()) {
            ruleOpt = botFlowRuleService.findDefault(estadoOrigen);
        }

        if (ruleOpt.isPresent()) {
            BotFlowRule rule = ruleOpt.get();
            Long idAntes = sesion.getCurrentState().getId();
            actionHandlerFactory.getHandler(rule.getActionType())
                    .ifPresent(handler -> handler.execute(sesion, rule, input));

            if (rule.getNextState() != null && sesion.getCurrentState().getId().equals(idAntes)) {
                sesion.setCurrentState(rule.getNextState());
            }
        } else {
            menuHandler.handle(sesion, input);
        }

        return (sesion.getCurrentState() != null) ? sesion.getCurrentState().getMessage() : null;
    }

    private boolean estaEnPeriodoDeBloqueo(SessionData data, LocalDateTime now) {
        String lastMenuTs = data.getExtraInfo().get("LAST_MENU_TS");
        if (lastMenuTs == null) return false;
        try {
            return LocalDateTime.parse(lastMenuTs).isAfter(now.minusHours(24));
        } catch (Exception e) {
            return false;
        }
    }

    private void actualizarTimestampMenu(SessionData data, LocalDateTime now) {
        data.addExtra("LAST_MENU_TS", now.toString());
    }

    private Optional<BotOpcion> botOpcionService_obtenerOpcion(BotState estado, String input) {
        return botStateRepository.findById(estado.getId())
                .flatMap(s -> s.getId() == 1L ? Optional.empty() : Optional.empty());
    }

    private String resetearAlMenuInicial(UsuarioSesion sesion) {
        BotState estadoInicial = botStateRepository.findById(1L).orElseThrow();
        sesion.setCurrentState(estadoInicial);

        SessionData data = sesion.getTempData();
        String ts = data.getExtraInfo().get("LAST_MENU_TS");
        SessionData newData = new SessionData();
        newData.addExtra("LAST_MENU_TS", ts);
        sesion.setTempData(newData);
        return estadoInicial.getMessage();
    }

    private String reemplazarEtiquetas(String mensaje, UsuarioSesion sesion) {
        if (mensaje == null) return "";
        SessionData data = sesion.getTempData();
        String nombreSector = (sesion.getSector() != null) ? sesion.getSector().getName() : "tu zona";
        String linkSector = (sesion.getSector() != null) ? sesion.getSector().getCalendarLink() : "";
        return mensaje.replace("{nombre}", nombreSector).replace("{link}", linkSector != null ? linkSector : "");
    }

    public void enviarMensajeManual(String phone, String content) {
        whatsappService.sendMessage(phone, content);
        registrarMensaje(phone, content, "OPERATOR");
        messagingTemplate.convertAndSend("/topic/updates", phone);
    }

    @Transactional
    public void actualizarEstadoBot(String phone, boolean enabled) {
        UsuarioSesion sesion = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);
        sesion.setBotEnabled(enabled);
        usuarioSesionService.save(sesion);
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

    private record RespuestaBot(String mensajeTexto, String templateName, String mediaId) {}
}
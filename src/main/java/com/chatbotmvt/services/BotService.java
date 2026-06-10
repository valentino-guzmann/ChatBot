package com.chatbotmvt.services;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.entity.*;
import com.chatbotmvt.handlers.ActionHandlerFactory;
import com.chatbotmvt.handlers.MenuHandler;
import com.chatbotmvt.repository.BotStateRepository;
import com.chatbotmvt.repository.MensajeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotService {

    private static final Set<String> SALUDOS = Set.of(
            "hola", "holaa", "holaaa", "holaaaa",
            "buenas", "buen dia", "buen día",
            "buenos dias", "buenos días",
            "buenas tardes", "buenas noches",
            "hey", "que tal", "qué tal",
            "hi", "hello"
    );

    private final UsuarioSesionService usuarioSesionService;
    private final MenuHandler menuHandler;
    private final BotFlowRuleService botFlowRuleService;
    private final BotOpcionService botOpcionService;
    private final ActionHandlerFactory actionHandlerFactory;
    private final BotStateRepository botStateRepository;
    private final WhatsappService whatsappService;
    private final MensajeLogRepository mensajeLogRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Async("botExecutor")
    public void procesarYResponder(String phone, String text) {
        try {
            UsuarioSesion sesion = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);

            registrarMensaje(phone, text, "USER", null);
            messagingTemplate.convertAndSend("/topic/updates", phone);

            if (sesion.getBotEnabled() != null && !sesion.getBotEnabled()) {
                log.info("🤖 Bot pausado para {}.", phone);
                return;
            }

            RespuestaBot resultado = ejecutarLogicaYGuardar(sesion, text);

            SessionData data = sesion.getTempData();
            if (data != null && data.getExtraInfo().containsKey("ignore_reply")) {
                log.info("🤫 Silencio aplicado para {}. No se envía respuesta.", phone);
                data.getExtraInfo().remove("ignore_reply");
                usuarioSesionService.save(sesion);
                return;
            }

            if (resultado != null && resultado.mensajeTexto() != null) {
                BotState estadoActual = sesion.getCurrentState();
                String messageId;

                if (estadoActual.getTemplateName() != null && !estadoActual.getTemplateName().isBlank()) {
                    messageId = whatsappService.sendTemplateWithAutoRefresh(phone, estadoActual, null);
                }
                else if (estadoActual.getMediaId() != null && !estadoActual.getMediaId().isBlank()) {
                    messageId = whatsappService.sendImageWithAutoRefresh(phone, estadoActual, resultado.mensajeTexto());
                }
                else {
                    messageId = whatsappService.sendMessage(phone, resultado.mensajeTexto());
                }

                registrarMensaje(phone, resultado.mensajeTexto(), "BOT", messageId);
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

        // Saludos: mostrar menú principal, respetando el bloqueo de 24hs
        if (esSaludo(input)) {
            if (estaEnPeriodoDeBloqueo(data, now)) {
                log.info("🤫 Saludo [{}] bloqueado por regla de 24hs", input);
                data.addExtra("ignore_reply", "true");
                return null;
            }
            log.info("👋 Saludo detectado [{}]. Mostrando menú principal.", input);
            BotState menuPrincipal = botStateRepository.findById(1L).orElse(null);
            if (menuPrincipal != null) {
                sesion.setCurrentState(menuPrincipal);
                actualizarTimestampMenu(data, now);
                return menuPrincipal.getMessage();
            }
        }

        if ((input.equals("menu") || input.equals("0")) && estadoOrigen.getId() == 1L) {
            if (estaEnPeriodoDeBloqueo(data, now)) {
                log.info("🤫 Bloqueando reenvío de menú por [{}] en período de 24hs", input);
                data.addExtra("ignore_reply", "true");
                return null;
            }
        }

        Optional<BotFlowRule> ruleOpt = botFlowRuleService.findExact(estadoOrigen, input);
        if (ruleOpt.isEmpty()) ruleOpt = botFlowRuleService.findDefault(estadoOrigen);

        if (ruleOpt.isPresent()) {
            data.getExtraInfo().remove("ignore_reply");
            BotFlowRule rule = ruleOpt.get();
            Long idAntes = sesion.getCurrentState().getId();
            actionHandlerFactory.getHandler(rule.getActionType()).ifPresent(h -> h.execute(sesion, rule, input));
            if (rule.getNextState() != null && sesion.getCurrentState().getId().equals(idAntes)) {
                sesion.setCurrentState(rule.getNextState());
            }
            if (sesion.getCurrentState().getId() == 1L && estaEnPeriodoDeBloqueo(data, now)) {
                log.info("🤫 Supresión de menú (estado 1) por regla de 24hs tras BotFlowRule");
                data.addExtra("ignore_reply", "true");
                return null;
            }
            return sesion.getCurrentState().getMessage();
        }

        var opcionOpt = botOpcionService.obtenerEstadoYOpcion(estadoOrigen, input);
        if (opcionOpt.isPresent()) {
            data.getExtraInfo().remove("ignore_reply");
            menuHandler.handle(sesion, input);
            if (sesion.getCurrentState().getId() == 1L && estaEnPeriodoDeBloqueo(data, now)) {
                log.info("🤫 Supresión de menú (estado 1) por regla de 24hs tras opción");
                data.addExtra("ignore_reply", "true");
                return null;
            }
            actualizarTimestampMenu(data, now);
            return sesion.getCurrentState().getMessage();
        }

        if (estadoOrigen.getId() != 1L) {
            BotState menuPrincipal = botStateRepository.findById(1L).orElse(null);
            var opcionGlobal = botOpcionService.obtenerEstadoYOpcion(menuPrincipal, input);

            if (opcionGlobal.isPresent()) {
                log.info("🌐 Salto Global a opción [{}]", input);
                sesion.setCurrentState(menuPrincipal);
                menuHandler.handle(sesion, input);
                if (sesion.getCurrentState().getId() == 1L && estaEnPeriodoDeBloqueo(data, now)) {
                    log.info("🤫 Supresión de menú (estado 1) por regla de 24hs tras salto global");
                    data.addExtra("ignore_reply", "true");
                    return null;
                }
                actualizarTimestampMenu(data, now);
                return sesion.getCurrentState().getMessage();
            }

            data.addExtra("ignore_reply", "true");
            return null;
        }

        if (estadoOrigen.getId() == 1L) {
            if (estaEnPeriodoDeBloqueo(data, now)) {
                log.info("🤫 Silenciando texto libre [{}] por regla de 24hs", input);
                data.addExtra("ignore_reply", "true");
                return null;
            } else {
                actualizarTimestampMenu(data, now);
                return estadoOrigen.getMessage();
            }
        }

        return null;
    }

    private boolean esSaludo(String input) {
        if (input == null || input.isBlank()) return false;
        String txt = input.trim().toLowerCase();
        if (SALUDOS.contains(txt)) return true;
        return txt.matches("hol+a+") || txt.matches("buen+as?");
    }

    private boolean estaEnPeriodoDeBloqueo(SessionData data, LocalDateTime now) {
        String lastMenuTs = data.getExtraInfo().get("LAST_MENU_TS");
        if (lastMenuTs == null) return false;
        try {
            LocalDateTime lastSent = LocalDateTime.parse(lastMenuTs);
            return lastSent.isAfter(now.minusHours(24));
        } catch (Exception e) {
            return false;
        }
    }

    private void actualizarTimestampMenu(SessionData data, LocalDateTime now) {
        String lastMenuTs = data.getExtraInfo().get("LAST_MENU_TS");
        if (lastMenuTs == null) {
            data.addExtra("LAST_MENU_TS", now.toString());
            return;
        }
        try {
            LocalDateTime lastSent = LocalDateTime.parse(lastMenuTs);
            if (!lastSent.isAfter(now.minusHours(24))) {
                data.addExtra("LAST_MENU_TS", now.toString());
            }
        } catch (Exception e) {
            data.addExtra("LAST_MENU_TS", now.toString());
        }
    }

    private String reemplazarEtiquetas(String mensaje, UsuarioSesion sesion) {
        if (mensaje == null) return "";
        String nombreSector = (sesion.getSector() != null) ? sesion.getSector().getName() : "tu zona";
        String linkSector = (sesion.getSector() != null) ? sesion.getSector().getCalendarLink() : "";
        return mensaje.replace("{nombre}", nombreSector).replace("{link}", linkSector != null ? linkSector : "");
    }

    public void enviarMensajeManual(String phone, String content) {
        String messageId = whatsappService.sendMessage(phone, content);
        registrarMensaje(phone, content, "OPERATOR", messageId);
        messagingTemplate.convertAndSend("/topic/updates", phone);
    }

    @Transactional
    public void actualizarEstadoBot(String phone, boolean enabled) {
        UsuarioSesion sesion = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);
        sesion.setBotEnabled(enabled);
        usuarioSesionService.save(sesion);
        messagingTemplate.convertAndSend("/topic/updates", phone);
    }

    private void registrarMensaje(String phone, String content, String sender, String messageId) {
        MensajeLog logEntry = new MensajeLog();
        logEntry.setPhone(phone);
        logEntry.setContent(content);
        logEntry.setSender(sender);
        logEntry.setMessageId(messageId);
        logEntry.setStatus(messageId != null ? "sent" : null);
        logEntry.setCreatedAt(OffsetDateTime.now());
        mensajeLogRepository.save(logEntry);
    }

    private record RespuestaBot(String mensajeTexto, String templateName, String mediaId) {}
}

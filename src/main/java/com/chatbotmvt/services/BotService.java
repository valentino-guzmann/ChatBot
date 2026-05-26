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
    private final BotOpcionService botOpcionService;
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

            if (sesion.getBotEnabled() != null && !sesion.getBotEnabled()) return;

            RespuestaBot resultado = ejecutarLogicaYGuardar(sesion, text);

            SessionData data = sesion.getTempData();
            if (data != null && data.getExtraInfo().containsKey("ignore_reply")) {
                log.info("🤫 Silencio: No se repite el menú para {}", phone);
                data.getExtraInfo().remove("ignore_reply");
                usuarioSesionService.save(sesion);
                return;
            }

            if (resultado != null && resultado.mensajeTexto() != null) {
                BotState estadoActual = sesion.getCurrentState();

                if (estadoActual.getMediaId() != null && !estadoActual.getMediaId().isBlank()) {
                    whatsappService.sendImageById(phone, estadoActual.getMediaId(), resultado.mensajeTexto());
                }
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
        String templateToSend = estadoNuevo.getTemplateName();
        String mediaIdToSend = estadoNuevo.getMediaId();

        return new RespuestaBot(mensajeFinal, templateToSend, mediaIdToSend);
    }

    private String procesarFlujoInterno(UsuarioSesion sesion, String message) {
        BotState estadoOrigen = sesion.getCurrentState();
        String input = (message == null) ? "" : message.trim().toLowerCase();
        SessionData data = sesion.getTempData();
        LocalDateTime now = LocalDateTime.now();

        if (input.equals("menu") || input.equals("0")) {
            if (estaEnPeriodoDeBloqueo(data, now)) {
                log.info("🚫 Reset con '0' bloqueado por 24hs.");
                data.addExtra("ignore_reply", "true");
                return null;
            }
            actualizarTimestampMenu(data, now);
            return resetearAlMenuInicial(sesion);
        }

        if (estadoOrigen.getId() == 1L) {
            Optional<BotOpcion> opt = botOpcionService.obtenerEstadoYOpcion(estadoOrigen, input);
            if (opt.isPresent()) {
                actualizarTimestampMenu(data, now);
                menuHandler.handle(sesion, input);
                return sesion.getCurrentState().getMessage();
            }

            if (estaEnPeriodoDeBloqueo(data, now)) {
                // Ya se le mostró el menú hoy, así que ignoramos cualquier otra palabra
                log.info("🤫 Silenciando 'hola' o texto libre por regla de 24hs.");
                data.addExtra("ignore_reply", "true");
                return null;
            } else {
                // Es su primer mensaje del día ("hola"), le mostramos el menú
                log.info("👋 'Hola' detectado fuera de periodo de bloqueo. Mostrando menú.");
                actualizarTimestampMenu(data, now);
                return estadoOrigen.getMessage();
            }
        }

        // 3. FLUJO NORMAL (Si ya salió del menú principal)
        // Aquí el bot responde normalmente a lo que el usuario escriba
        Optional<BotFlowRule> ruleOpt = botFlowRuleService.findExact(estadoOrigen, input);
        if (ruleOpt.isEmpty()) ruleOpt = botFlowRuleService.findDefault(estadoOrigen);

        if (ruleOpt.isPresent()) {
            BotFlowRule rule = ruleOpt.get();
            Long idAntes = sesion.getCurrentState().getId();
            actionHandlerFactory.getHandler(rule.getActionType()).ifPresent(h -> h.execute(sesion, rule, input));
            if (rule.getNextState() != null && sesion.getCurrentState().getId().equals(idAntes)) {
                sesion.setCurrentState(rule.getNextState());
            }
            return sesion.getCurrentState().getMessage();
        } else {
            menuHandler.handle(sesion, input);
            if (data.getExtraInfo().containsKey("ignore_reply")) return null;
            return sesion.getCurrentState().getMessage();
        }
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

    private boolean estaEnPeriodoDeBloqueo(SessionData data, LocalDateTime now) {
        String lastMenuTs = data.getExtraInfo().get("LAST_MENU_TS");
        if (lastMenuTs == null) return false;
        return LocalDateTime.parse(lastMenuTs).isAfter(now.minusHours(24));
    }

    private void actualizarTimestampMenu(SessionData data, LocalDateTime now) {
        data.addExtra("LAST_MENU_TS", now.toString());
    }

    private record RespuestaBot(String mensajeTexto, String templateName, String mediaId) {}
}
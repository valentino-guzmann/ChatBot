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
import java.util.List;
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

    private static final Set<Long> ESTADOS_DIRECCION_RECLAMO = Set.of(4L, 5L, 30L, 31L, 21L, 33L);

    private final UsuarioSesionService usuarioSesionService;
    private final MenuHandler menuHandler;
    private final BotFlowRuleService botFlowRuleService;
    private final BotOpcionService botOpcionService;
    private final ActionHandlerFactory actionHandlerFactory;
    private final BotStateRepository botStateRepository;
    private final WhatsappService whatsappService;
    private final ReclamoService reclamoService;
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

        // 2. Reclamo simplificado: en estados de dirección, cualquier texto crea el reclamo y vuelve al menú
        if (ESTADOS_DIRECCION_RECLAMO.contains(estadoOrigen.getId())
                && !input.isBlank()
                && !input.equals("menu")
                && !input.equals("0")
                && !esSaludo(input)) {
            log.info("📝 Reclamo simplificado desde estado [{}]. Dirección: [{}]", estadoOrigen.getId(), input);
            return procesarReclamoSimple(sesion, input, data, now);
        }

        // 3. Estado 23L (selección de zona Bolsones/Desperdicios) + "0" → volver al menú con solo opciones
        if (estadoOrigen.getId() == 23L && input.equals("0")) {
            log.info("🔙 Volviendo al menú desde selección de zona (estado 23L)");
            return volverAlMenuConOpciones(sesion, data, now);
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

    private String procesarReclamoSimple(UsuarioSesion sesion, String input, SessionData data, LocalDateTime now) {
        data.setDireccion(input);

        String tipo = data.getTipoReclamo();
        if (tipo == null || tipo.isBlank()) {
            tipo = "GENERAL";
        }

        String descripcion = String.format("Dirección: %s", input);
        reclamoService.crearReclamo(sesion.getPhone(), tipo, descripcion, sesion.getSector());
        log.info("✅ Reclamo simple creado para [{}] tipo=[{}] direccion=[{}]", sesion.getPhone(), tipo, input);

        // Limpiar datos temporales del reclamo
        data.setDireccion(null);
        data.setReferencia(null);
        data.setTipoReclamo(null);
        data.setPendingSectorId(null);

        // Volver al menú principal
        BotState menuPrincipal = botStateRepository.findById(1L).orElse(null);
        if (menuPrincipal != null) {
            sesion.setCurrentState(menuPrincipal);
            actualizarTimestampMenu(data, now);
            return menuPrincipal.getMessage();
        }
        return null;
    }

    private String volverAlMenuConOpciones(UsuarioSesion sesion, SessionData data, LocalDateTime now) {
        BotState menuPrincipal = botStateRepository.findById(1L).orElse(null);
        if (menuPrincipal == null) {
            return null;
        }

        sesion.setCurrentState(menuPrincipal);
        actualizarTimestampMenu(data, now);

        // Construir mensaje solo con las opciones (sin el encabezado completo del menú)
        var opciones = botOpcionService.obtenerOpciones(menuPrincipal);
        if (opciones.isEmpty()) {
            return menuPrincipal.getMessage();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("¿En qué puedo ayudarte?").append("\n\n");

        for (var opcion : opciones) {
            sb.append(opcion.getOptionKey()).append(" ").append(opcion.getDescription()).append("\n");
        }

        return sb.toString().trim();
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
        registrarMensaje(phone, content, "text", sender, messageId, null, null, null);
    }

    private void registrarMensaje(String phone, String content, String type, String sender, String messageId,
                                  String mediaId, String mediaUrl, String mimeType) {
        MensajeLog logEntry = new MensajeLog();
        logEntry.setPhone(phone);
        logEntry.setContent(content);
        logEntry.setType(type);
        logEntry.setSender(sender);
        logEntry.setMessageId(messageId);
        logEntry.setMediaId(mediaId);
        logEntry.setMediaUrl(mediaUrl);
        logEntry.setMimeType(mimeType);
        logEntry.setStatus(messageId != null ? "sent" : null);
        logEntry.setCreatedAt(OffsetDateTime.now());
        mensajeLogRepository.save(logEntry);
    }

    @Async("botExecutor")
    public void procesarImagenEntrante(String phone, String mediaId, String mimeType, String caption) {
        try {
            UsuarioSesion sesion = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);

            String mediaUrl = whatsappService.downloadAndSaveImage(mediaId, phone);
            String content = caption != null ? caption : "[Imagen recibida]";

            registrarMensaje(phone, content, "image", "USER", null, mediaId, mediaUrl, mimeType);
            messagingTemplate.convertAndSend("/topic/updates", phone);

            if (sesion.getBotEnabled() != null && !sesion.getBotEnabled()) {
                log.info("🤖 Bot pausado para {}. Imagen solo registrada.", phone);
                return;
            }

            // Por ahora, si recibe una imagen, simplemente responde con un mensaje amigable
            // o podrías conectar esto con tu lógica de flujo si necesitas manejar imágenes
            log.info("🖼️ Imagen recibida de {}. MediaUrl: {}", phone, mediaUrl);

        } catch (Exception e) {
            log.error("❌ Error procesando imagen entrante: {}", e.getMessage(), e);
        }
    }

    private record RespuestaBot(String mensajeTexto, String templateName, String mediaId) {}
}

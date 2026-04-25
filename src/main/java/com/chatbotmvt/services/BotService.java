package com.chatbotmvt.services;

import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.handlers.InputHandler;
import com.chatbotmvt.handlers.MenuHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotService {

    private final UsuarioSesionService usuarioSesionService;
    private final MenuHandler menuHandler;
    private final InputHandler inputHandler;
    private final BotOpcionService botOpcionService;

    public String procesarMensaje(String phone, String message) {

        log.info("📩 Mensaje recibido de [{}]: {}", phone, message);

        boolean esNuevo = false;

        var existente = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);

        log.info("👤 Usuario [{}] en estado: {}", phone, existente.getCurrentState().getName());

        if (existente.getStep() == 0 && existente.getTempData() == null) {
            esNuevo = true;
            log.info("🆕 Usuario [{}] detectado como nuevo → se envió saludo", phone);
        }

        if (esNuevo) {
            log.info("⏭️ Se omite procesamiento para [{}] (primer mensaje)", phone);
            return null;
        }

        var estado = existente.getCurrentState();
        String input = message == null ? "" : message.trim();

        log.info("🔎 Input normalizado para [{}]: '{}'", phone, input);

        if (estado.getType().name().equals("MENU")) {

            log.info("📋 Procesando como MENU → estado: {}", estado.getName());
            menuHandler.handle(existente, input);

        } else if (estado.getType().name().equals("INPUT")) {

            log.info("✏️ Procesando como INPUT → estado: {}", estado.getName());
            inputHandler.handle(existente, input);
        }

        usuarioSesionService.save(existente);

        var nuevoEstado = existente.getCurrentState();

        log.info("🔄 Usuario [{}] cambió a estado: {}", phone, nuevoEstado.getName());

        String response = construirRespuesta(nuevoEstado);

        log.info("📤 Respuesta generada para [{}]: \n{}", phone, response);

        return response;
    }

    private String construirRespuesta(BotState estado) {

        log.info("🛠️ Construyendo respuesta para estado: {}", estado.getName());

        StringBuilder response = new StringBuilder();

        response.append(estado.getMessage()).append("\n\n");

        if (estado.getType().name().equals("MENU")) {

            var opciones = botOpcionService.obtenerOpciones(estado);

            log.info("📊 Estado {} tiene {} opciones", estado.getName(), opciones.size());

            for (var op : opciones) {
                response.append(op.getOptionKey())
                        .append("️⃣ ")
                        .append(op.getDescription())
                        .append("\n");
            }
        }

        return response.toString();
    }
}
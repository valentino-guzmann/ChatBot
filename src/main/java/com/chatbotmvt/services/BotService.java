package com.chatbotmvt.services;

import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.handlers.InputHandler;
import com.chatbotmvt.handlers.MenuHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotService {

    private final UsuarioSesionService usuarioSesionService;
    private final MenuHandler menuHandler;
    private final InputHandler inputHandler;
    private final BotOpcionService botOpcionService;
    private final BotStateService botStateService;

    public String procesarMensaje(String phone, String message) {

        log.info("📩 Mensaje recibido de [{}]: {}", phone, message);

        UsuarioSesion usuario = usuarioSesionService.obtenerOCrearUsuarioSesion(phone);

        String input = message == null ? "" : message.trim();
        BotState estado = usuario.getCurrentState();

        log.info("👤 Usuario [{}] en estado: {}", phone, estado.getName());

        if (input.equalsIgnoreCase("menu")) {

            usuario.setCurrentState(usuarioSesionService.obtenerEstadoInicial());
            usuario.setStep(0);
            usuario.setTempData(null);

            usuarioSesionService.save(usuario);

            return construirRespuesta(usuario, usuario.getCurrentState());
        }

        if (estado.getName().equals("CONFIRMACION")) {

            if (input.equals("1")) {

                log.info("✅ Confirmado: {}", usuario.getTempData());

                usuario.setTempData(null);
                usuario.setStep(0);
                usuario.setCurrentState(usuarioSesionService.obtenerEstadoInicial());

            } else if (input.equals("2")) {

                log.info("🔄 Reingresar datos");

                usuario.setTempData(null);
                usuario.setStep(0);
                usuario.setCurrentState(
                        botStateService.findByName("INPUT_DESMALEZADO")
                );
            }

            usuarioSesionService.save(usuario);
            return construirRespuesta(usuario, usuario.getCurrentState());
        }

        if (estado.getType().name().equals("MENU")) {

            menuHandler.handle(usuario, input);

        } else if (estado.getType().name().equals("INPUT")) {

            inputHandler.handle(usuario, input);
        }

        usuarioSesionService.save(usuario);

        return construirRespuesta(usuario, usuario.getCurrentState());
    }

    private String construirRespuesta(UsuarioSesion usuario, BotState estado) {

        StringBuilder response = new StringBuilder();

        response.append(estado.getMessage()).append("\n\n");

        if ("error".equals(usuario.getTempData())) {
            response.append("❌ Opción inválida, intenta nuevamente\n\n");
        }

        if ("error_input".equals(usuario.getTempData())) {
            response.append("❌ Ingresa un dato válido\n\n");
        }

        if (estado.getType().name().equals("MENU")) {

            var opciones = botOpcionService.obtenerOpciones(estado);

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
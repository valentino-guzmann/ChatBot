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

        BotState estado = usuario.getCurrentState();
        String input = message == null ? "" : message.trim();

        log.info("👤 Usuario [{}] en estado: {}", phone, estado.getName());

        if (input.equalsIgnoreCase("menu")) {

            usuario.setCurrentState(usuarioSesionService.obtenerEstadoInicial());
            usuario.setStep(0);
            usuario.setTempData(null);

            usuarioSesionService.save(usuario);

            return construirRespuesta(usuario);
        }

        if (estado.getName().equals("CONFIRMACION")) {

            if (input.equals("1")) {

                log.info("✅ Confirmado: {}", usuario.getTempData());

                usuario.setTempData(null);
                usuario.setStep(0);

                usuario.setCurrentState(
                        usuarioSesionService.obtenerEstadoInicial()
                );

            } else if (input.equals("2")) {

                log.info("🔄 Reingresar datos");

                usuario.setTempData(null);
                usuario.setStep(0);

                usuario.setCurrentState(
                        botStateService.findByName("INPUT_DESMALEZADO")
                );
            }
        }
        else if (estado.getType().name().equals("MENU")) {

            menuHandler.handle(usuario, input);

        } else if (estado.getType().name().equals("INPUT")) {

            inputHandler.handle(usuario, input);
        }

        usuarioSesionService.save(usuario);

        return construirRespuesta(usuario);
    }

    private String construirRespuesta(UsuarioSesion usuario) {

        BotState estado = usuario.getCurrentState();
        StringBuilder response = new StringBuilder();

        String mensaje = estado.getMessage();

        if (mensaje.contains("{DATOS}") && usuario.getTempData() != null) {
            mensaje = mensaje.replace("{DATOS}", usuario.getTempData());
        }

        response.append(mensaje).append("\n\n");

        if ("error".equals(usuario.getTempData())) {
            response.append("❌ Opción inválida, intenta nuevamente\n\n");
        }

        return response.toString();
    }
}
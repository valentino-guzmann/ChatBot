package com.chatbotmvt.handlers;

import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.services.BotOpcionService;
import com.chatbotmvt.services.BotStateService;
import com.chatbotmvt.services.SectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MenuHandler {

    private final BotOpcionService botOpcionService;
    private final SectorService sectorService;
    private final BotStateService botStateService;

    public void handle(UsuarioSesion sesion, String message) {

        var estado = sesion.getCurrentState();

        if (estado.getName().equals("SELECCION_ZONA_BOLSONES")
                || estado.getName().equals("SELECCION_ZONA_DESPERDICIOS")) {

            switch (message) {
                case "1" -> sesion.setSector(sectorService.getById(1L));
                case "2" -> sesion.setSector(sectorService.getById(2L));
                case "3" -> sesion.setSector(sectorService.getById(3L));
                case "4" -> sesion.setSector(sectorService.getById(4L));
            }

            sesion.setCurrentState(
                    botStateService.findByName("CONFIRMAR_ZONA")
            );

            return;
        }

        var opcion = botOpcionService.obtenerEstadoYOpcion(estado, message);

        if (opcion.isPresent()) {

            log.info("✅ Opción válida [{}]", message);

            sesion.setCurrentState(opcion.get().getNextState());
            sesion.setTempData(null);

        } else {

            if (message.matches("\\d+")) {
                log.warn("❌ Opción inválida numérica: {}", message);
                sesion.setTempData("error");
            } else {
                log.info("💬 Texto libre ignorado: {}", message);
                sesion.setTempData(null);
            }
        }
    }
}
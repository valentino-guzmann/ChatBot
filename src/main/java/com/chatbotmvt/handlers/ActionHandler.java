package com.chatbotmvt.handlers;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.repository.BotStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActionHandler implements BotActionHandler {
    @Override
    public String getActionType() { return "SET_TYPE"; }

    @Override
    public String execute(UsuarioSesion sesion, BotFlowRule rule, String input) {
        SessionData data = sesion.getTempData();
        String tipo = rule.getActionValue();

        data.setTipoReclamo(tipo);

        if (sesion.getSector() == null) {
            return switch (tipo) {
                case "BOLSONES" -> "🛍️ *Bolsones verdes*\n\n📍 Info en: www.venadotuerto.gob.ar/bolsonesverdes\n\n📩 También podés consultarnos enviando dirección y referencia.\n\n🚫 IMPORTANTE: No colocar escombros.";
                case "BARRIDO" -> "🧹 *Barrido*\n\nInfo de barrido...";
                default -> "Has seleccionado " + tipo;
            };
        }
        return null;
    }
}
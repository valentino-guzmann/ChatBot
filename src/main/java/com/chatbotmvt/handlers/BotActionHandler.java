package com.chatbotmvt.handlers;

import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.BotOpcion;
import com.chatbotmvt.entity.UsuarioSesion;

public interface BotActionHandler {
    String getActionType();

    String execute(UsuarioSesion sesion, BotFlowRule rule, String input);

    default String executeFromOption(UsuarioSesion sesion, BotOpcion opcion, String input) {
        return null;
    }

}
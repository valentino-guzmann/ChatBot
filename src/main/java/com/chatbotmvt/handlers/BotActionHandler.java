package com.chatbotmvt.handlers;

import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.UsuarioSesion;

public interface BotActionHandler {
    String getActionType();

    String execute(UsuarioSesion sesion, BotFlowRule rule, String input);
}
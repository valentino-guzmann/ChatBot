package com.chatbotmvt.handlers;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ActionHandlerFactory {
    private final Map<String, BotActionHandler> handlers = new HashMap<>();

    public ActionHandlerFactory(List<BotActionHandler> handlerList) {
        handlerList.forEach(handler -> handlers.put(handler.getActionType(), handler));
    }

    public Optional<BotActionHandler> getHandler(String actionType) {
        return Optional.ofNullable(handlers.get(actionType));
    }
}

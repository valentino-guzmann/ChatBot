package com.chatbotmvt.services;

import com.chatbotmvt.entity.BotOpcion;
import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.repository.BotOpcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BotOpcionService {
    private final BotOpcionRepository botOpcionRepository;

    public Optional<BotOpcion> obtenerEstadoYOpcion(BotState estadoActual, String message) {
        return botOpcionRepository.findByStateAndOptionKey(estadoActual, message);
    }
}

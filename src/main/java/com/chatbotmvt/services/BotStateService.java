package com.chatbotmvt.services;

import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.repository.BotStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BotStateService {

    private final BotStateRepository botStateRepository;

    public BotState findByName(String name) {
        return botStateRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Estado no encontrado: " + name));
    }
}

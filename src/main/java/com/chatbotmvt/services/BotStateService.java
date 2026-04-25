package com.chatbotmvt.services;

import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.entity.Usuario;
import com.chatbotmvt.repository.BotStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BotStateService {

    private final BotStateRepository botStateRepository;

    public BotState getMenuState() {
        return botStateRepository.findByName("MENU_PRINCIPAL")
                .orElseThrow(() -> new RuntimeException("Estado MENU no encontrado"));
    }

    public Usuario crearUsuario(String phone) {

        BotState estadoActual = getMenuState();

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setPhone(phone);
        nuevoUsuario.setCurrentState(estadoActual);

        return nuevoUsuario;
    }
}

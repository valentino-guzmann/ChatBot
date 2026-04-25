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

    public BotState getMenuState() {
        return botStateRepository.findByName("MENU_PRINCIPAL")
                .orElseThrow(() -> new RuntimeException("Estado MENU no encontrado"));
    }

    public UsuarioSesion crearUsuario(String phone) {
        BotState estadoActual = getMenuState();

        var nuevoUsuario = new UsuarioSesion();
        nuevoUsuario.setPhone(phone);
        nuevoUsuario.setCurrentState(estadoActual);

        return nuevoUsuario;
    }
}

package com.chatbotmvt.services;

import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.entity.Usuario;
import com.chatbotmvt.repository.BotStateRepository;
import com.chatbotmvt.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final BotStateRepository botStateRepository;

    public Usuario obtenerOCrearUsuario(String phone) {
        return usuarioRepository.findByPhone(phone)
                .orElseGet(() -> crearUsuario(phone));
    }

    private Usuario crearUsuario(String phone) {

        BotState estadoInicial = botStateRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Estado MENU_PRINCIPAL no encontrado"));

        Usuario usuario = new Usuario();
        usuario.setPhone(phone);
        usuario.setCurrentState(estadoInicial);

        return usuarioRepository.save(usuario);
    }

    public BotState obtenerEstadoPorId(Long id) {
        return botStateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado no encontrado"));
    }
}
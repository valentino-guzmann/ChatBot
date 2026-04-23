package com.chatbotmvt.services;

import com.chatbotmvt.entity.Usuario;
import com.chatbotmvt.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final BotStateService botStateService;

    public Usuario obtenerOCrearUsuario(String phone) {
        return usuarioRepository.findByPhone(phone)
                .orElseGet(() -> {
                    Usuario nuevoUsuario = botStateService.crearUsuario(phone);
                    return usuarioRepository.save(nuevoUsuario);
                });
    }
}

package com.chatbotmvt.services;

import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.repository.BotStateRepository;
import com.chatbotmvt.repository.UsuarioSesionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioSesionService {

    private final UsuarioSesionRepository usuarioSesionRepository;
    private final BotStateRepository botStateRepository;

    @Transactional
    public UsuarioSesion obtenerOCrearUsuarioSesion(String phone) {
        return usuarioSesionRepository.findByPhone(phone)
                .orElseGet(() -> {
                    var estadoInicial = botStateRepository.findById(1L)
                            .orElseThrow(() -> new RuntimeException("Error: No existe el estado inicial 1 en la DB"));

                    var nuevaSesion = new UsuarioSesion();
                    nuevaSesion.setPhone(phone);
                    nuevaSesion.setCurrentState(estadoInicial);

                    return usuarioSesionRepository.save(nuevaSesion);
                });
    }

    public void save(UsuarioSesion sesion) {
        usuarioSesionRepository.save(sesion);
    }
}
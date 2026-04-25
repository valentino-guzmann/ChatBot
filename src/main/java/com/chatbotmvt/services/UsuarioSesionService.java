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
    private final WhatsappService whatsappService;

    @Transactional
    public UsuarioSesion obtenerOCrearUsuarioSesion(String phone) {

        var existente = usuarioSesionRepository.findByPhone(phone);

        if (existente.isPresent()) {
            return existente.get();
        }

        var estadoInicial = botStateRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("No existe estado inicial"));

        var nueva = new UsuarioSesion();
        nueva.setPhone(phone);
        nueva.setCurrentState(estadoInicial);
        nueva.setStep(0);

        UsuarioSesion saved = usuarioSesionRepository.save(nueva);

        whatsappService.sendSaludoSeguro(phone);

        return saved;
    }

    public void save(UsuarioSesion sesion) {
        usuarioSesionRepository.save(sesion);
    }

    public boolean esNuevoUsuario(String phone) {
        return usuarioSesionRepository.findByPhone(phone).isEmpty();
    }
}
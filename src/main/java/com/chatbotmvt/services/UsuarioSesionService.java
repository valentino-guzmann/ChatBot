package com.chatbotmvt.services;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.dto.UsuarioDTO;
import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.repository.BotStateRepository;
import com.chatbotmvt.repository.UsuarioSesionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioSesionService {

    private final UsuarioSesionRepository usuarioSesionRepository;
    private final BotStateRepository botStateRepository;

    public UsuarioSesion obtenerOCrearUsuarioSesion(String phone) {

        return usuarioSesionRepository.findByPhone(phone)
                .orElseGet(() -> {

                    BotState estadoInicial = botStateRepository.findById(1L)
                            .orElseThrow(() -> new RuntimeException("No existe estado inicial"));

                    UsuarioSesion nueva = new UsuarioSesion();
                    nueva.setPhone(phone);
                    nueva.setCurrentState(estadoInicial);
                    nueva.setTempData(new SessionData());

                    return usuarioSesionRepository.save(nueva);
                });
    }

    public BotState obtenerEstadoInicial() {
        return botStateRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("No existe estado inicial"));
    }

    public void save(UsuarioSesion sesion) {
        usuarioSesionRepository.save(sesion);
    }

    public List<UsuarioDTO> obtenerTodos() {
        return usuarioSesionRepository.findAll().stream()
                .map(u -> new UsuarioDTO(
                        u.getPhone(),
                        u.getCurrentState() != null ? u.getCurrentState().getId() : null,
                        u.getSector() != null ? u.getSector().getName() : "Sin asignar",
                        u.getCreated_at(),
                        u.getUpdated_at()))
                .toList();
    }
}
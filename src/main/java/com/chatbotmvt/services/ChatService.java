package com.chatbotmvt.services;

import com.chatbotmvt.dto.UsuarioDTO;
import com.chatbotmvt.entity.MensajeLog;
import com.chatbotmvt.repository.MensajeLogRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final UsuarioSesionService usuarioSesionService;
    private final MensajeLogRepository logRepository;
    private final BotService botService;

    public List<UsuarioDTO> obtenerTodosLosChats() {
        return usuarioSesionService.obtenerTodos();
    }

    public List<MensajeLog> obtenerHistorial(String phone) {
        return logRepository.findByPhoneOrderByCreatedAtAsc(phone);
    }

    @Transactional
    public void marcarComoLeido(String phone) {
        logRepository.markAsReadByOperator(phone);
    }

    public void enviarMensajeManual(String phone, String content) {
        botService.enviarMensajeManual(phone, content);
    }

    public void actualizarEstadoBot(String phone, boolean enabled) {
        botService.actualizarEstadoBot(phone, enabled);
    }
}

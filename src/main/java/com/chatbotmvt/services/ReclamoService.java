package com.chatbotmvt.services;

import com.chatbotmvt.entity.Reclamo;
import com.chatbotmvt.entity.Sector;
import com.chatbotmvt.repository.ReclamoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReclamoService {

    private final ReclamoRepository reclamoRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public List<Reclamo> obtenerReclamos() {
        return reclamoRepository.findAll();
    }

    public Reclamo obtenerPorPhone(String phone) {
        return reclamoRepository.findByPhone(phone).orElseThrow(() -> new RuntimeException("Reclamo no encontrado"));
    }

    @Transactional
    public void crearReclamo(String phone, String tipo, String data, Sector sector) {
        Reclamo r = Reclamo.builder()
                .phone(phone)
                .tipo(tipo)
                .descripcion(data)
                .sector(sector != null ? sector.getName() : "Sin sector")
                .estado(Reclamo.EstadoReclamo.PENDIENTE)
                .build();

        reclamoRepository.save(r);

        // Notificar al Dashboard que hay un reclamo nuevo
        messagingTemplate.convertAndSend("/topic/updates", "nuevo_reclamo");
    }

    @Transactional
    public void actualizarEstado(Long id, String nuevoEstado) {
        Reclamo r = reclamoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reclamo no encontrado con ID: " + id));

        r.setEstado(Reclamo.EstadoReclamo.valueOf(nuevoEstado.toUpperCase()));
        reclamoRepository.save(r);

        messagingTemplate.convertAndSend("/topic/updates", "estado_actualizado");
    }
}
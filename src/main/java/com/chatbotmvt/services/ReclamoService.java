package com.chatbotmvt.services;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.entity.Reclamo;
import com.chatbotmvt.entity.Sector;
import com.chatbotmvt.repository.ReclamoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReclamoService {

    private final ReclamoRepository reclamoRepository;

    public void crearReclamo(String phone, String tipo, SessionData data, Sector sector) {

        String direccion = (data.getDireccion() == null || data.getDireccion().isBlank())
                ? "Sin dirección"
                : data.getDireccion();

        String referencia = (data.getReferencia() == null || data.getReferencia().isBlank())
                ? "Sin referencia"
                : data.getReferencia();

        String descripcion = "📍 Dirección: " + direccion +
                " | 📌 Ref: " + referencia;

        Reclamo r = new Reclamo();
        r.setPhone(phone);
        r.setTipo(tipo);
        r.setDescripcion(descripcion);

        if (sector != null) {
            r.setSector(sector.getName());
        }

        reclamoRepository.save(r);
    }
}
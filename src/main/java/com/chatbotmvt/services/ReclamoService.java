package com.chatbotmvt.services;

import com.chatbotmvt.entity.Reclamo;
import com.chatbotmvt.repository.ReclamoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReclamoService {

    private final ReclamoRepository reclamoRepository;

    public void crearReclamo(String phone, String tipo, String data) {

        Reclamo r = new Reclamo();

        r.setPhone(phone);
        r.setTipo(tipo);
        r.setDescripcion(data);

        reclamoRepository.save(r);
    }
}
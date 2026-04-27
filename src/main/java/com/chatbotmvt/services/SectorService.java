package com.chatbotmvt.services;

import com.chatbotmvt.entity.Sector;
import com.chatbotmvt.repository.SectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SectorService {

    private final SectorRepository repository;

    public Sector getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sector no encontrado"));
    }
}
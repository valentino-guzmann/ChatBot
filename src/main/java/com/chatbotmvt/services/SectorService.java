package com.chatbotmvt.services;

import com.chatbotmvt.entity.Sector;
import com.chatbotmvt.entity.SectorDetail;
import com.chatbotmvt.repository.SectorDetailRepository;
import com.chatbotmvt.repository.SectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SectorService {

    private final SectorRepository sectorRepository;
    private final SectorDetailRepository sectorDetailRepository;

    public Sector findById(Long id) {
        return sectorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sector no encontrado"));
    }

    public List<SectorDetail> getDetalles(Sector sector) {
        return sectorDetailRepository.findBySector(sector);
    }
}
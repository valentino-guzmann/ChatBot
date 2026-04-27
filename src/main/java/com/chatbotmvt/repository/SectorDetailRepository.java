package com.chatbotmvt.repository;

import com.chatbotmvt.entity.Sector;
import com.chatbotmvt.entity.SectorDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectorDetailRepository extends JpaRepository<SectorDetail, Long> {

    List<SectorDetail> findBySector(Sector sector);
}
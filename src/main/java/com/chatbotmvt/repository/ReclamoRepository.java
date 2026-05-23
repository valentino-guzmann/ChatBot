package com.chatbotmvt.repository;

import com.chatbotmvt.entity.Reclamo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReclamoRepository extends JpaRepository<Reclamo, Long> {
    Optional<Reclamo> findByPhone(String phone);
}

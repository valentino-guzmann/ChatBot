package com.chatbotmvt.repository;

import com.chatbotmvt.entity.MensajeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MensajeLogRepository extends JpaRepository<MensajeLog, Long> {
    List<MensajeLog> findByPhoneOrderByCreatedAtAsc(String phone);

    Optional<MensajeLog> findFirstByPhoneOrderByCreatedAtDesc(String phone);
}
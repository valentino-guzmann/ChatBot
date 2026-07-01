package com.chatbotmvt.repository;

import com.chatbotmvt.entity.MensajeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface MensajeLogRepository extends JpaRepository<MensajeLog, Long> {
    List<MensajeLog> findByPhoneOrderByCreatedAtAsc(String phone);

    Optional<MensajeLog> findFirstByPhoneOrderByCreatedAtDesc(String phone);

    Optional<MensajeLog> findByMessageId(String messageId);

    @Query("SELECT m FROM MensajeLog m WHERE m.mediaId IS NOT NULL AND (m.mediaUrl LIKE '/uploads/%' OR m.mediaUrl IS NULL)")
    List<MensajeLog> findRecoverableImages();
}
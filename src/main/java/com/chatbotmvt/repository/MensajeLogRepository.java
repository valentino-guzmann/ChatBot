package com.chatbotmvt.repository;

import com.chatbotmvt.entity.MensajeLog;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MensajeLogRepository extends JpaRepository<MensajeLog, Long> {
    List<MensajeLog> findByPhoneOrderByCreatedAtAsc(String phone);

    Optional<MensajeLog> findFirstByPhoneOrderByCreatedAtDesc(String phone);

    Optional<MensajeLog> findByMessageId(String messageId);

    @Query("SELECT m FROM MensajeLog m WHERE m.mediaId IS NOT NULL AND (m.mediaUrl LIKE '/uploads/%' OR m.mediaUrl IS NULL)")
    List<MensajeLog> findRecoverableImages();

    @Query("SELECT COUNT(m) FROM MensajeLog m WHERE m.phone = :phone AND m.sender = 'USER' AND m.readByOperator = false")
    long countUnreadByPhone(@Param("phone") String phone);

    @Modifying
    @Transactional
    @Query("UPDATE MensajeLog m SET m.readByOperator = true WHERE m.phone = :phone AND m.sender = 'USER'")
    void markAsReadByOperator(@Param("phone") String phone);
}
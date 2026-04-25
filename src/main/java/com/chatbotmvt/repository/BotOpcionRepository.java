package com.chatbotmvt.repository;

import com.chatbotmvt.entity.BotOpcion;
import com.chatbotmvt.entity.BotState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BotOpcionRepository extends JpaRepository<BotOpcion, Long> {
    Optional<BotOpcion> findByStateAndOptionKey(BotState estadoActual, String message);

    List<BotOpcion> findByState(BotState estado);
}

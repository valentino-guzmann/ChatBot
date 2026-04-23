package com.chatbotmvt.repository;

import com.chatbotmvt.entity.BotState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BotStateRepository extends JpaRepository<BotState, Long> {
    Optional<BotState> findByName(String menu);
}

package com.chatbotmvt.repository;

import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.BotState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BotFlowRuleRepository extends JpaRepository<BotFlowRule, Long> {

    Optional<BotFlowRule> findByStateAndInputPattern(BotState state, String inputPattern);
}
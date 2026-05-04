package com.chatbotmvt.services;

import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.repository.BotFlowRuleRepository;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BotFlowRuleService {

    private final BotFlowRuleRepository repository;
    private final Cache<String, BotFlowRule> botFlowRuleCache;

    public Optional<BotFlowRule> find(BotState state, String input) {

        String safe = input == null ? "" : input.trim();
        String key = state.getId() + ":" + safe.toLowerCase();

        BotFlowRule rule = botFlowRuleCache.get(key, k ->
                repository.findByStateAndInputPattern(state, safe)
                        .or(() -> repository.findByStateAndInputPattern(state, "default"))
                        .orElse(null)
        );

        return Optional.ofNullable(rule);
    }
}
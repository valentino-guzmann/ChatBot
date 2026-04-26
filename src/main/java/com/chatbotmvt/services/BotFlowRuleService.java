package com.chatbotmvt.services;

import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.repository.BotFlowRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;@Service
@RequiredArgsConstructor
public class BotFlowRuleService {

    private final BotFlowRuleRepository repository;

    public Optional<BotFlowRule> find(BotState state, String input) {

        String safe = input == null ? "" : input.trim();

        return repository.findByStateAndInputPattern(state, safe)
                .or(() -> repository.findByStateAndInputPattern(state, "default"));
    }
}
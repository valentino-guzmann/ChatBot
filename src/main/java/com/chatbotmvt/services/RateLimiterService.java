package com.chatbotmvt.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class RateLimiterService {

    private final Cache<String, Integer> requestCounts = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(10000)
            .build();

    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    public boolean isAllowed(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return true; // Si no hay identificación, dejamos pasar (lo maneja la firma)
        }
        Integer count = requestCounts.getIfPresent(clientId);
        if (count == null) {
            requestCounts.put(clientId, 1);
            return true;
        }
        if (count >= MAX_REQUESTS_PER_MINUTE) {
            log.warn("🚫 Rate limit excedido para clientId: {}", clientId);
            return false;
        }
        requestCounts.put(clientId, count + 1);
        return true;
    }
}

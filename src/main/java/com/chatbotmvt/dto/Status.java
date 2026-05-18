package com.chatbotmvt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Status(
        String id,
        String status,
        Long timestamp,
        @JsonProperty("recipient_id") String recipientId,
        Conversation conversation,
        Pricing pricing
) {}
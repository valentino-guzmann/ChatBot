package com.chatbotmvt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageReceived(
        String id,
        String from,
        TextMessage text,
        ImageMessage image,
        String type
) {}

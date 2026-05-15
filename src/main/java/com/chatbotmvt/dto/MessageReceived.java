package com.chatbotmvt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageReceived(
        String from,
        TextMessage text,
        String type
) {}
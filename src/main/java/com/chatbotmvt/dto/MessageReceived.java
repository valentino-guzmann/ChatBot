package com.chatbotmvt.dto;

public record MessageReceived(
        String from,
        TextMessage text
) {}
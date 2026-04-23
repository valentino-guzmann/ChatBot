package com.chatbotmvt.dto;

public record SendMessageRequest(
        String messaging_product,
        String to,
        String type,
        Text text
) {}
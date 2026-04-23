package com.chatbotmvt.dto;

import java.util.List;

public record Value(
        List<MessageReceived> messages
) {}

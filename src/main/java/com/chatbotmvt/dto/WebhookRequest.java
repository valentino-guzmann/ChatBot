package com.chatbotmvt.dto;

import java.util.List;

public record WebhookRequest(
        List<Entry> entry
) {}

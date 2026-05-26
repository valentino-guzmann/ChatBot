package com.chatbotmvt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record WhatsappResponse(
        @JsonProperty("messages") List<Map<String, String>> messages
) {}

package com.chatbotmvt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Value(
        @JsonProperty("messaging_product") String messagingProduct,
        List<MessageReceived> messages
) {}

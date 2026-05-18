package com.chatbotmvt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Contact(
        @JsonProperty("profile") Profile profile,
        @JsonProperty("wa_id") String waId
) {}
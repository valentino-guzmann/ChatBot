package com.chatbotmvt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Pricing(
        @JsonProperty("billable") boolean billable,
        @JsonProperty("pricing_model") String pricingModel,
        String category
) {}
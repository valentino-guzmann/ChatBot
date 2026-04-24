package com.chatbotmvt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${access.token}")
    private String accessToken;

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl("https://graph.facebook.com/v25.0")
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();
    }
}
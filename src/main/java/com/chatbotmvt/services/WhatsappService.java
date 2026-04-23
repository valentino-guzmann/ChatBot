package com.chatbotmvt.services;

import com.chatbotmvt.dto.SendMessageRequest;
import com.chatbotmvt.dto.Text;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class WhatsappService {

    private final RestClient restClient;

    @Value("${whatsapp.phone-id}")
    private String phoneNumberId;

    public void sendMessage(String phone, String message) {

        if (phone == null || phone.isBlank()) return;
        if (message == null || message.isBlank()) return;

        System.out.println("Enviando mensaje a: " + phone + " -> " + message);

        var request = new SendMessageRequest(
                "whatsapp",
                phone,
                "text",
                new Text(message)
        );

        restClient.post()
                .uri("/{phoneId}/messages", phoneNumberId)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    var body = new String(res.getBody().readAllBytes());
                    System.out.println("ERROR WHATSAPP: " + body);
                    throw new RuntimeException(body);
                })
                .toBodilessEntity();
    }
}

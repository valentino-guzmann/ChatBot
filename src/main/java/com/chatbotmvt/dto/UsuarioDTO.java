package com.chatbotmvt.dto;

import java.time.LocalDateTime;

public record UsuarioDTO(
        String phone,
        Long currentStateId,
        String sector,
        Boolean botEnabled,
        String lastMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

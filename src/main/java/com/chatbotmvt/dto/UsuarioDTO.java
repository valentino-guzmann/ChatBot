package com.chatbotmvt.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record UsuarioDTO(
        String phone,
        Long currentStateId,
        String sector,
        Boolean botEnabled,
        String lastMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

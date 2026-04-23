package com.chatbotmvt.dto;

import java.time.LocalDateTime;

public record UsuarioDTO(
        String phone,
        Long current_state_id,
        String sector,
        LocalDateTime created_at,
        LocalDateTime updated_at
) {
}

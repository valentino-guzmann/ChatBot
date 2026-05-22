package com.chatbotmvt.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensaje_log")
@Data
public class MensajeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String sender; // "USER", "BOT", "OPERATOR"

    private LocalDateTime createdAt;
}
package com.chatbotmvt.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phone;

    @Lob
    private String message;

    @Enumerated(EnumType.STRING)
    private MessageDirection direction; // IN / OUT

    private String stateName;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum MessageDirection {
        IN, OUT
    }
}


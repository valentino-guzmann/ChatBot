package com.chatbotmvt.entity;

import jakarta.persistence.*;
        import lombok.Data;
import java.time.OffsetDateTime;

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

    private String sender;
    private String type;

    @Column(name = "media_id")
    private String mediaId;

    @Column(name = "media_url", length = 2048)
    private String mediaUrl;

    private String caption;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "read_by_operator", nullable = false)
    private Boolean readByOperator = false;

    @Column(name = "message_id", unique = true)
    private String messageId;

    @Column(name = "status")
    private String status;

    private OffsetDateTime createdAt;
}
package com.chatbotmvt.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "reclamos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reclamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private String tipo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    private String sector;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoReclamo estado = EstadoReclamo.PENDIENTE;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public enum EstadoReclamo {
        PENDIENTE, RESUELTO, CANCELADO
    }
}
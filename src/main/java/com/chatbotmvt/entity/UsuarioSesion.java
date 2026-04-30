package com.chatbotmvt.entity;

import com.chatbotmvt.config.SessionDataConverter;
import com.chatbotmvt.dto.SessionData;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_session")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioSesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phone;

    @ManyToOne
    @JoinColumn(name = "current_state_id")
    private BotState currentState;

    @ManyToOne
    @JoinColumn(name = "sector_id")
    private Sector sector;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = SessionDataConverter.class)
    private SessionData tempData;

    private LocalDateTime created_at;
    private LocalDateTime updated_at;

    @PrePersist
    public void prePersist() {
        this.created_at = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updated_at = LocalDateTime.now();
    }
}

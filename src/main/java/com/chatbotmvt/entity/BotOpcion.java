package com.chatbotmvt.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bot_option")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BotOpcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "state_id")
    private BotState state;

    private String optionKey;
    private String description;

    @ManyToOne
    @JoinColumn(name = "next_state_id")
    private BotState nextState;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "action_value")
    private String actionValue;
}

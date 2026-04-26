package com.chatbotmvt.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bot_flow_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BotFlowRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "state_id")
    private BotState state;

    @Column(name = "input_pattern")
    private String inputPattern;

    @ManyToOne
    @JoinColumn(name = "next_state_id")
    private BotState nextState;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "action_value")
    private String actionValue;
}

package com.chatbotmvt.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sector")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Sector {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String dia;
    private String semana;

    private String calendarLink;
    private String imageUrl;
}
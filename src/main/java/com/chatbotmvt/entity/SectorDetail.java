package com.chatbotmvt.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sector_detail")
@Getter
@Setter
public class SectorDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sector_id")
    private Sector sector;

    @Column(columnDefinition = "TEXT")
    private String descripcion;
}
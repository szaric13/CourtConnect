package com.courtconnect.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    private String position;      // PG, SG, SF, PF, C

    private Integer heightCm;

    private Double rating = 0.0;

    private Integer gamesPlayed = 0;
}
package com.courtconnect.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerStatsResponse {
    private String username;
    private String position;
    private Integer heightCm;
    private Double rating;
    private Integer gamesPlayed;
    private Integer wins;
    private Integer losses;
    private Integer totalPoints;
}
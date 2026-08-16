package com.courtconnect.match.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScoreUpdateResponse {
    private String team;
    private Integer scoreTeamA;
    private Integer scoreTeamB;
    private String player;
    private Integer points;
    private String message;  // npr. "Strahinja +2"
}
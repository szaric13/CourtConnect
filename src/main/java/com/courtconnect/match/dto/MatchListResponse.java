package com.courtconnect.match.dto;

import com.courtconnect.match.MatchStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MatchListResponse {
    private Long id;
    private String court;
    private LocalDateTime dateTime;
    private Integer playersNeeded;
    private String skillLevel;
    private MatchStatus status;
    private String creatorUsername;
    private Integer participantCount;
}
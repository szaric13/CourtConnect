package com.courtconnect.match.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ScoreUpdateRequest {
    @NotBlank
    private String team;    // "A" ili "B"

    @NotNull
    @Min(1)
    @Max(3)
    private Integer points; // 1, 2 ili 3
}
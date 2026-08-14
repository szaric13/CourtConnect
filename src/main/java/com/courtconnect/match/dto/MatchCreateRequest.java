package com.courtconnect.match.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MatchCreateRequest {
    @NotBlank
    private String court;

    @NotNull
    private LocalDateTime dateTime;

    @NotNull
    @Min(2)
    @Max(20)
    private Integer playersNeeded;

    @NotBlank
    private String skillLevel;
}
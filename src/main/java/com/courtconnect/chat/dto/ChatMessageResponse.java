package com.courtconnect.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageResponse {
    private Long id;
    private String username;
    private String content;
    private LocalDateTime timestamp;
}
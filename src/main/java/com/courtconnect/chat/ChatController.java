package com.courtconnect.chat;

import com.courtconnect.chat.dto.ChatMessageRequest;
import com.courtconnect.chat.dto.ChatMessageResponse;
import com.courtconnect.match.Match;
import com.courtconnect.match.MatchRepository;
import com.courtconnect.user.User;
import com.courtconnect.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new RuntimeException("Match not found"));

        ChatMessage message = new ChatMessage();
        message.setMatch(match);
        message.setUser(user);
        message.setContent(request.getContent());
        chatMessageRepository.save(message);

        ChatMessageResponse response = ChatMessageResponse.builder()
                .id(message.getId())
                .username(username)
                .content(request.getContent())
                .timestamp(message.getTimestamp())
                .build();

        messagingTemplate.convertAndSend("/topic/match/" + match.getId() + "/chat", response);
    }
    @GetMapping("/api/matches/{matchId}/chat/history")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(@PathVariable Long matchId) {
        List<ChatMessageResponse> history = chatMessageRepository
                .findByMatchIdOrderByTimestampAsc(matchId)
                .stream()
                .map(msg -> ChatMessageResponse.builder()
                        .id(msg.getId())
                        .username(msg.getUser().getUsername())
                        .content(msg.getContent())
                        .timestamp(msg.getTimestamp())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }
}
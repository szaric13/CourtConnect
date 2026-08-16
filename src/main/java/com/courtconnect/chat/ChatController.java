package com.courtconnect.chat;

import com.courtconnect.chat.dto.ChatMessageRequest;
import com.courtconnect.chat.dto.ChatMessageResponse;
import com.courtconnect.match.Match;
import com.courtconnect.match.MatchRepository;
import com.courtconnect.user.User;
import com.courtconnect.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

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
}
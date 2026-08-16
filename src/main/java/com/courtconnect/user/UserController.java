package com.courtconnect.user;

import com.courtconnect.user.dto.PlayerStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/{username}/stats")
    public ResponseEntity<PlayerStatsResponse> getPlayerStats(@PathVariable String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(mapToPlayerStats(user));
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<PlayerStatsResponse>> getRanking() {
        List<PlayerStatsResponse> ranking = userRepository.findAll().stream()
                .sorted((u1, u2) -> {
                    // Sortiranje po wins opadajuće, pa totalPoints opadajuće
                    int winCompare = u2.getWins().compareTo(u1.getWins());
                    if (winCompare != 0) return winCompare;
                    return u2.getTotalPoints().compareTo(u1.getTotalPoints());
                })
                .map(this::mapToPlayerStats)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ranking);
    }

    private PlayerStatsResponse mapToPlayerStats(User user) {
        return PlayerStatsResponse.builder()
                .username(user.getUsername())
                .position(user.getPosition())
                .heightCm(user.getHeightCm())
                .rating(user.getRating())
                .gamesPlayed(user.getGamesPlayed())
                .wins(user.getWins())
                .losses(user.getLosses())
                .totalPoints(user.getTotalPoints())
                .build();
    }
}
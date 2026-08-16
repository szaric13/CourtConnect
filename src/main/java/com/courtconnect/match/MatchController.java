package com.courtconnect.match;

import com.courtconnect.match.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @PostMapping
    public ResponseEntity<MatchResponse> createMatch(@RequestBody MatchCreateRequest request,
                                                     Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(matchService.createMatch(request, username));
    }

    @GetMapping
    public ResponseEntity<List<MatchListResponse>> getAllMatches() {
        return ResponseEntity.ok(matchService.getAllMatches());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchResponse> getMatch(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.getMatch(id));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<MatchResponse> joinMatch(@PathVariable Long id,
                                                   Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(matchService.joinMatch(id, username));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<MatchResponse> leaveMatch(@PathVariable Long id,
                                                    Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(matchService.leaveMatch(id, username));
    }
    @PostMapping("/{id}/team/{team}")
    public ResponseEntity<MatchResponse> assignTeam(@PathVariable Long id,
                                                    @PathVariable String team,
                                                    Authentication authentication) {
        return ResponseEntity.ok(matchService.assignTeam(id, team, authentication.getName()));
    }

    @PostMapping("/{id}/ready")
    public ResponseEntity<MatchResponse> toggleReady(@PathVariable Long id,
                                                     Authentication authentication) {
        return ResponseEntity.ok(matchService.toggleReady(id, authentication.getName()));
    }
    @PostMapping("/{id}/score")
    public ResponseEntity<ScoreUpdateResponse> updateScore(@PathVariable Long id,
                                                           @RequestBody ScoreUpdateRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(matchService.addScore(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/finish")
    public ResponseEntity<MatchResponse> finishMatch(@PathVariable Long id,
                                                     Authentication authentication) {
        return ResponseEntity.ok(matchService.finishMatch(id, authentication.getName()));
    }
}
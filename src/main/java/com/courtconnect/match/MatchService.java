package com.courtconnect.match;

import com.courtconnect.match.dto.MatchCreateRequest;
import com.courtconnect.match.dto.MatchListResponse;
import com.courtconnect.match.dto.MatchResponse;
import com.courtconnect.user.User;
import com.courtconnect.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    @Transactional
    public MatchResponse createMatch(MatchCreateRequest request, String username) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Match match = new Match();
        match.setCourt(request.getCourt());
        match.setDateTime(request.getDateTime());
        match.setPlayersNeeded(request.getPlayersNeeded());
        match.setSkillLevel(request.getSkillLevel());
        match.setStatus(MatchStatus.OPEN);
        match.setCreator(creator);
        match.getParticipants().add(creator);  // kreator se automatski pridružuje

        match = matchRepository.save(match);
        return mapToMatchResponse(match);
    }

    @Transactional(readOnly = true)
    public List<MatchListResponse> getAllMatches() {
        return matchRepository.findAll().stream()
                .map(this::mapToMatchListResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MatchResponse getMatch(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        return mapToMatchResponse(match);
    }

    @Transactional
    public MatchResponse joinMatch(Long id, String username) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (match.getStatus() != MatchStatus.OPEN) {
            throw new RuntimeException("Match is not open for joining");
        }
        if (match.getParticipants().contains(user)) {
            throw new RuntimeException("User already in match");
        }

        match.getParticipants().add(user);
        if (match.getParticipants().size() >= match.getPlayersNeeded()) {
            match.setStatus(MatchStatus.FULL);
        }

        return mapToMatchResponse(matchRepository.save(match));
    }

    @Transactional
    public MatchResponse leaveMatch(Long id, String username) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!match.getParticipants().contains(user)) {
            throw new RuntimeException("User not in match");
        }
        if (match.getCreator().equals(user)) {
            throw new RuntimeException("Creator cannot leave match, delete it instead");
        }

        match.getParticipants().remove(user);
        if (match.getStatus() == MatchStatus.FULL && match.getParticipants().size() < match.getPlayersNeeded()) {
            match.setStatus(MatchStatus.OPEN);
        }

        return mapToMatchResponse(matchRepository.save(match));
    }

    private MatchResponse mapToMatchResponse(Match match) {
        List<String> participants = match.getParticipants().stream()
                .map(User::getUsername)
                .collect(Collectors.toList());

        return MatchResponse.builder()
                .id(match.getId())
                .court(match.getCourt())
                .dateTime(match.getDateTime())
                .playersNeeded(match.getPlayersNeeded())
                .skillLevel(match.getSkillLevel())
                .status(match.getStatus())
                .creatorUsername(match.getCreator().getUsername())
                .participantCount(match.getParticipants().size())
                .participants(participants)
                .build();
    }

    private MatchListResponse mapToMatchListResponse(Match match) {
        return MatchListResponse.builder()
                .id(match.getId())
                .court(match.getCourt())
                .dateTime(match.getDateTime())
                .playersNeeded(match.getPlayersNeeded())
                .skillLevel(match.getSkillLevel())
                .status(match.getStatus())
                .creatorUsername(match.getCreator().getUsername())
                .participantCount(match.getParticipants().size())
                .build();
    }
}
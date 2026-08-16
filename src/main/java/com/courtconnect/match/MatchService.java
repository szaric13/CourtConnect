package com.courtconnect.match;

import com.courtconnect.match.dto.*;
import com.courtconnect.user.User;
import com.courtconnect.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

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
        messagingTemplate.convertAndSend("/topic/match/" + match.getId(), mapToMatchResponse(match));
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

        match = matchRepository.save(match);
        messagingTemplate.convertAndSend("/topic/match/" + match.getId(), mapToMatchResponse(match));
        return mapToMatchResponse(match);
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

        match = matchRepository.save(match);
        messagingTemplate.convertAndSend("/topic/match/" + match.getId(), mapToMatchResponse(match));
        return mapToMatchResponse(match);
    }

    @Transactional
    public MatchResponse assignTeam(Long id, String team, String username) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (match.getStatus() != MatchStatus.OPEN && match.getStatus() != MatchStatus.FULL) {
            throw new RuntimeException("Match already started");
        }

        // Ukloni igrača iz oba tima, pa dodaj u izabrani
        match.getTeamA().remove(user);
        match.getTeamB().remove(user);
        if ("A".equalsIgnoreCase(team)) {
            match.getTeamA().add(user);
        } else if ("B".equalsIgnoreCase(team)) {
            match.getTeamB().add(user);
        } else {
            throw new RuntimeException("Invalid team");
        }

        match = matchRepository.save(match);
        messagingTemplate.convertAndSend("/topic/match/" + match.getId(), mapToMatchResponse(match));
        return mapToMatchResponse(match);
    }

    @Transactional
    public MatchResponse toggleReady(Long id, String username) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (match.getStatus() != MatchStatus.OPEN && match.getStatus() != MatchStatus.FULL) {
            throw new RuntimeException("Match already started");
        }

        if (match.getTeamA().contains(user)) {
            match.setTeamAReady(!match.getTeamAReady());
        } else if (match.getTeamB().contains(user)) {
            match.setTeamBReady(!match.getTeamBReady());
        } else {
            throw new RuntimeException("User not in any team");
        }

        // ✅ Kada su oba tima spremna, meč prelazi u LIVE
        if (match.getTeamAReady() && match.getTeamBReady()) {
            match.setStatus(MatchStatus.LIVE);
        }

        match = matchRepository.save(match);
        messagingTemplate.convertAndSend("/topic/match/" + match.getId(), mapToMatchResponse(match));
        return mapToMatchResponse(match);
    }

    @Transactional
    public ScoreUpdateResponse addScore(Long id, ScoreUpdateRequest request, String username) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (match.getStatus() != MatchStatus.LIVE) {
            throw new RuntimeException("Match is not live");
        }

        if ("A".equalsIgnoreCase(request.getTeam())) {
            match.setScoreTeamA(match.getScoreTeamA() + request.getPoints());
        } else if ("B".equalsIgnoreCase(request.getTeam())) {
            match.setScoreTeamB(match.getScoreTeamB() + request.getPoints());
        } else {
            throw new RuntimeException("Invalid team");
        }

        match = matchRepository.save(match);

        ScoreUpdateResponse response = ScoreUpdateResponse.builder()
                .team(request.getTeam().toUpperCase())
                .scoreTeamA(match.getScoreTeamA())
                .scoreTeamB(match.getScoreTeamB())
                .player(username)
                .points(request.getPoints())
                .message(username + " +" + request.getPoints())
                .build();

        messagingTemplate.convertAndSend("/topic/match/" + match.getId() + "/score", response);
        return response;
    }

    // ✅ Dodato scoreTeamA i scoreTeamB u mapToMatchResponse
    private MatchResponse mapToMatchResponse(Match match) {
        List<String> participants = match.getParticipants().stream()
                .map(User::getUsername)
                .collect(Collectors.toList());

        List<String> teamA = match.getTeamA().stream()
                .map(User::getUsername)
                .collect(Collectors.toList());

        List<String> teamB = match.getTeamB().stream()
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
                .teamA(teamA)
                .teamB(teamB)
                .teamAReady(match.getTeamAReady())
                .teamBReady(match.getTeamBReady())
                .scoreTeamA(match.getScoreTeamA())
                .scoreTeamB(match.getScoreTeamB())
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
                .scoreTeamA(match.getScoreTeamA())
                .scoreTeamB(match.getScoreTeamB())
                .build();
    }
    @Transactional
    public MatchResponse finishMatch(Long id, String username) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        // Provera da li je meč LIVE (ili FULL ako nije ni počeo?)
        if (match.getStatus() != MatchStatus.LIVE) {
            throw new RuntimeException("Match is not live, cannot finish");
        }

        // Odredi pobednika
        if (match.getScoreTeamA() > match.getScoreTeamB()) {
            // Tim A pobedio
            updateStatsForTeam(match, true);
        } else if (match.getScoreTeamB() > match.getScoreTeamA()) {
            // Tim B pobedio
            updateStatsForTeam(match, false);
        } else {
            // Nerešeno – tretiraj kao nerešeno ili ne ažuriraj win/loss
            // Za sada ne radimo ništa (ili svi gube? bolje ništa)
        }

        // Postavi status FINISHED
        match.setStatus(MatchStatus.FINISHED);
        match = matchRepository.save(match);

        // Obavesti sve
        messagingTemplate.convertAndSend("/topic/match/" + match.getId(), mapToMatchResponse(match));
        return mapToMatchResponse(match);
    }

    private void updateStatsForTeam(Match match, boolean teamAWon) {
        Set<User> winners = teamAWon ? match.getTeamA() : match.getTeamB();
        Set<User> losers = teamAWon ? match.getTeamB() : match.getTeamA();

        for (User user : winners) {
            user.setWins(user.getWins() + 1);
            user.setGamesPlayed(user.getGamesPlayed() + 1);
            user.setTotalPoints(user.getTotalPoints() + (teamAWon ? match.getScoreTeamA() : match.getScoreTeamB()));
            userRepository.save(user);
        }

        for (User user : losers) {
            user.setLosses(user.getLosses() + 1);
            user.setGamesPlayed(user.getGamesPlayed() + 1);
            user.setTotalPoints(user.getTotalPoints() + (teamAWon ? match.getScoreTeamB() : match.getScoreTeamA()));
            userRepository.save(user);
        }
    }
}
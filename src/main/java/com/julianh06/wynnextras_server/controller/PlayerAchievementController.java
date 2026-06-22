package com.julianh06.wynnextras_server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.julianh06.wynnextras_server.dto.PlayerAchievementDto;
import com.julianh06.wynnextras_server.entity.PlayerAchievement;
import com.julianh06.wynnextras_server.repository.PlayerAchievementRepository;
import com.julianh06.wynnextras_server.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/achievements")
public class PlayerAchievementController {
    private static final Logger logger = LoggerFactory.getLogger(PlayerAchievementController.class);

    @Autowired
    private PlayerAchievementRepository achievementRepo;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    @Transactional
    public ResponseEntity<?> uploadAchievements(
            @RequestBody PlayerAchievementDto.UploadRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createResponse("error", "Missing session token"));
        }

        AuthService.SessionData session = AuthService.validateSession(token);

        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createResponse("error", "Session expired or invalid"));
        }

        if (request.getAchievements() == null) {
            return ResponseEntity.badRequest().body("No achievements provided");
        }

        for (PlayerAchievementDto.AchievementData achievement : request.getAchievements()) {
            if (achievement.getId() == null || achievement.getId().isBlank()) {
                return ResponseEntity.badRequest().body("Achievement id must not be empty");
            }
        }

        try {
            String verifiedUuid = session.uuid;
            String verifiedUsername = session.username;

            achievementRepo.deleteByPlayerUuid(verifiedUuid);

            for (PlayerAchievementDto.AchievementData achievement : request.getAchievements()) {
                achievementRepo.save(new PlayerAchievement(
                        verifiedUuid,
                        verifiedUsername,
                        achievement.getId(),
                        defaultString(achievement.getTitle()),
                        defaultString(achievement.getDescription()),
                        normalizeType(achievement),
                        achievement.isSecret(),
                        achievement.isUnlocked(),
                        toInstant(achievement.getUnlockedAt()),
                        Math.max(0, achievement.getCurrent()),
                        achievement.getTarget(),
                        achievement.getCurrentLevel(),
                        serializeLevelTargets(achievement.getLevelTargets()),
                        request.getModVersion()
                ));
            }

            logger.info("Saved {} achievements for verified player {} (UUID: {})",
                    request.getAchievements().size(), verifiedUsername, verifiedUuid);

            return ResponseEntity.ok(createResponse("success", "Achievements uploaded successfully"));
        } catch (Exception e) {
            logger.error("Error saving achievements", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createResponse("error", "Failed to save achievements"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAchievements(@RequestParam String playerUuid) {
        String normalizedUuid = normalizeUuid(playerUuid);
        if (normalizedUuid == null) {
            return ResponseEntity.badRequest().body("Invalid UUID format");
        }

        List<PlayerAchievement> achievements = achievementRepo.findByPlayerUuidOrderByAchievementIdAsc(normalizedUuid);

        if (achievements.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No achievements found for player");
        }

        PlayerAchievement first = achievements.get(0);
        long unlockedCount = achievements.stream().filter(PlayerAchievement::isUnlocked).count();
        long updatedAt = achievements.stream()
                .map(PlayerAchievement::getUpdatedAt)
                .max(Instant::compareTo)
                .orElse(first.getUpdatedAt())
                .toEpochMilli();

        PlayerAchievementDto.PlayerAchievementsResponse response = new PlayerAchievementDto.PlayerAchievementsResponse(
                normalizedUuid,
                first.getPlayerName(),
                first.getModVersion(),
                updatedAt,
                unlockedCount,
                achievements.stream().map(this::toDto).collect(Collectors.toList())
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard(@RequestParam(defaultValue = "5") int limit) {
        if (limit < 1 || limit > 100) {
            return ResponseEntity.badRequest().body("Limit must be between 1 and 100");
        }

        try {
            List<Object[]> results = achievementRepo.findAchievementLeaderboard(PageRequest.of(0, limit));

            List<PlayerAchievementDto.LeaderboardEntry> leaderboard = results.stream()
                    .map(row -> new PlayerAchievementDto.LeaderboardEntry(
                            (String) row[0],
                            (String) row[1],
                            ((Number) row[2]).longValue(),
                            ((Instant) row[3]).toEpochMilli()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(leaderboard);
        } catch (Exception e) {
            logger.error("Error fetching achievement leaderboard", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch achievement leaderboard");
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> getAllPlayers() {
        try {
            List<Object[]> results = achievementRepo.findAllPlayersWithAchievements();

            List<PlayerAchievementDto.PlayerListEntry> players = results.stream()
                    .map(row -> new PlayerAchievementDto.PlayerListEntry(
                            (String) row[0],
                            (String) row[1],
                            (String) row[2],
                            ((Instant) row[3]).toEpochMilli(),
                            ((Number) row[4]).longValue()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(players);
        } catch (Exception e) {
            logger.error("Error fetching achievement player list", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch achievement player list");
        }
    }

    private PlayerAchievementDto.AchievementData toDto(PlayerAchievement achievement) {
        return new PlayerAchievementDto.AchievementData(
                achievement.getAchievementId(),
                achievement.getTitle(),
                achievement.getDescription(),
                achievement.getType(),
                achievement.isSecret(),
                achievement.isUnlocked(),
                achievement.getUnlockedAt() == null ? null : achievement.getUnlockedAt().toEpochMilli(),
                achievement.getCurrentProgress(),
                achievement.getTargetProgress(),
                achievement.getCurrentLevel(),
                deserializeLevelTargets(achievement.getLevelTargetsJson())
        );
    }

    private String normalizeUuid(String playerUuid) {
        if (playerUuid == null) return null;
        String normalizedUuid = playerUuid.replace("-", "").toLowerCase();
        return normalizedUuid.matches("[0-9a-f]{32}") ? normalizedUuid : null;
    }

    private Instant toInstant(Long epochMillis) {
        return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis);
    }

    private String serializeLevelTargets(List<Integer> levelTargets) {
        if (levelTargets == null) return null;
        try {
            return objectMapper.writeValueAsString(levelTargets);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid level targets", e);
        }
    }

    private List<Integer> deserializeLevelTargets(String levelTargetsJson) {
        if (levelTargetsJson == null || levelTargetsJson.isBlank()) return null;
        try {
            return objectMapper.readValue(
                    levelTargetsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class)
            );
        } catch (JsonProcessingException e) {
            logger.warn("Could not deserialize achievement level targets", e);
            return null;
        }
    }

    private String normalizeType(PlayerAchievementDto.AchievementData achievement) {
        if (achievement.getType() != null && !achievement.getType().isBlank()) {
            return achievement.getType();
        }
        if (achievement.getLevelTargets() != null && !achievement.getLevelTargets().isEmpty()) {
            return "tiered";
        }
        if (achievement.getTarget() != null) {
            return "progress";
        }
        return "simple";
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private Map<String, String> createResponse(String status, String message) {
        Map<String, String> response = new HashMap<>();
        response.put("status", status);
        response.put("message", message);
        return response;
    }
}

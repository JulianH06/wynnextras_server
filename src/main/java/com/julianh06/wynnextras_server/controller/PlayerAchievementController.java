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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        List<NormalizedAchievement> achievements = normalizeUploadAchievements(request);
        if (achievements.isEmpty()) {
            return ResponseEntity.badRequest().body("No achievements provided");
        }

        Set<String> achievementIds = new HashSet<>();
        for (NormalizedAchievement achievement : achievements) {
            if (achievement.data().getId() == null || achievement.data().getId().isBlank()) {
                return ResponseEntity.badRequest().body("Achievement id must not be empty");
            }
            if (!achievementIds.add(achievement.data().getId())) {
                return ResponseEntity.badRequest().body("Duplicate achievement id: " + achievement.data().getId());
            }
            String validationError = validateAchievement(achievement);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(validationError);
            }
        }

        try {
            String verifiedUuid = session.uuid;
            String verifiedUsername = session.username;

            achievementRepo.deleteByPlayerUuid(verifiedUuid);

            for (NormalizedAchievement normalizedAchievement : achievements) {
                PlayerAchievementDto.AchievementData achievement = normalizedAchievement.data();
                achievementRepo.save(new PlayerAchievement(
                        verifiedUuid,
                        verifiedUsername,
                        achievement.getId(),
                        defaultString(achievement.getTitle()),
                        defaultString(achievement.getDescription()),
                        normalizedAchievement.type(),
                        achievement.isSecret(),
                        achievement.isUnlocked(),
                        toInstant(achievement.getUnlockedAt()),
                        Math.max(0, achievement.getCurrent()),
                        targetProgress(normalizedAchievement),
                        currentLevel(normalizedAchievement),
                        serializeLevelTargets(normalizedAchievement.levelTargets()),
                        request.getModVersion()
                ));
            }

            logger.info("Saved {} achievements for verified player {} (UUID: {})",
                    achievements.size(), verifiedUsername, verifiedUuid);

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

        Map<String, List<PlayerAchievementDto.AchievementData>> groupedAchievements = groupAchievements(achievements);

        PlayerAchievementDto.PlayerAchievementsResponse response = new PlayerAchievementDto.PlayerAchievementsResponse(
                normalizedUuid,
                first.getPlayerName(),
                first.getModVersion(),
                updatedAt,
                unlockedCount,
                groupedAchievements.get("simple"),
                groupedAchievements.get("progress"),
                groupedAchievements.get("tiered")
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
                achievement.getUnlockedAt() == null ? null : achievement.getUnlockedAt().toString(),
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

    private Instant toInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid unlockedAt value: " + value, e);
        }
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

    private List<NormalizedAchievement> normalizeUploadAchievements(PlayerAchievementDto.UploadRequest request) {
        List<NormalizedAchievement> achievements = new ArrayList<>();

        if (request.getAchievements() != null) {
            for (PlayerAchievementDto.AchievementData achievement : request.getAchievements()) {
                achievements.add(new NormalizedAchievement(achievement, "simple", null));
            }
        }
        if (request.getProgressAchievements() != null) {
            for (PlayerAchievementDto.AchievementData achievement : request.getProgressAchievements()) {
                achievements.add(new NormalizedAchievement(achievement, "progress", achievement.getLevelTargets()));
            }
        }
        if (request.getTieredAchievements() != null) {
            for (PlayerAchievementDto.AchievementData achievement : request.getTieredAchievements()) {
                achievements.add(new NormalizedAchievement(achievement, "tiered", achievement.getLevelTargets()));
            }
        }

        return achievements;
    }

    private String validateAchievement(NormalizedAchievement achievement) {
        String type = achievement.type();
        if (!type.equals("simple") && !type.equals("progress") && !type.equals("tiered")) {
            return "Unsupported achievement type: " + type;
        }
        if (type.equals("progress") && (achievement.data().getTarget() == null || achievement.data().getTarget() <= 0)) {
            return "Progress achievement target must be greater than zero: " + achievement.data().getId();
        }
        if (type.equals("tiered")) {
            List<Integer> targets = achievement.levelTargets();
            if (targets == null || targets.isEmpty()) {
                return "Tiered achievement levelTargets must not be empty: " + achievement.data().getId();
            }
            if (targets.stream().anyMatch(target -> target == null || target <= 0)) {
                return "Tiered achievement levelTargets must be positive: " + achievement.data().getId();
            }
        }
        return null;
    }

    private Integer targetProgress(NormalizedAchievement achievement) {
        if (!achievement.type().equals("progress")) return null;
        return achievement.data().getTarget();
    }

    private Integer currentLevel(NormalizedAchievement achievement) {
        if (!achievement.type().equals("tiered")) return null;
        if (achievement.data().getCurrentLevel() != null) {
            return Math.min(Math.max(0, achievement.data().getCurrentLevel()), achievement.levelTargets().size());
        }
        List<Integer> targets = achievement.levelTargets();
        int current = Math.max(0, achievement.data().getCurrent());
        int level = 0;
        while (level < targets.size() && current >= targets.get(level)) {
            level++;
        }
        return level;
    }

    private Map<String, List<PlayerAchievementDto.AchievementData>> groupAchievements(List<PlayerAchievement> achievements) {
        Map<String, List<PlayerAchievementDto.AchievementData>> grouped = new HashMap<>();
        grouped.put("simple", new ArrayList<>());
        grouped.put("progress", new ArrayList<>());
        grouped.put("tiered", new ArrayList<>());

        for (PlayerAchievement achievement : achievements) {
            String type = achievement.getType() == null ? "simple" : achievement.getType();
            if (!grouped.containsKey(type)) {
                type = "simple";
            }
            grouped.get(type).add(toDto(achievement));
        }

        return grouped;
    }

    private record NormalizedAchievement(PlayerAchievementDto.AchievementData data, String type, List<Integer> levelTargets) {}

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

package com.julianh06.wynnextras_server.controller;

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

        String requestError = validateUploadRequest(request);
        if (requestError != null) {
            return ResponseEntity.badRequest().body(requestError);
        }

        Set<String> achievementIds = new HashSet<>();
        for (PlayerAchievementDto.AchievementState achievement : request.getAchievements()) {
            if (achievement == null) {
                return ResponseEntity.badRequest().body("Achievement entries must not be null");
            }
            if (achievement.getId() == null || achievement.getId().isBlank()) {
                return ResponseEntity.badRequest().body("Achievement id must not be empty");
            }
            String achievementId = achievement.getId().trim();
            if (!achievementIds.add(achievementId)) {
                return ResponseEntity.badRequest().body("Duplicate achievement id: " + achievementId);
            }
            if (achievement.getUnlocked() == null) {
                return ResponseEntity.badRequest().body("Achievement unlocked must be provided: " + achievementId);
            }
            if (achievement.getCurrent() != null && achievement.getCurrent() < 0) {
                return ResponseEntity.badRequest().body("Achievement current must not be negative: " + achievementId);
            }
        }

        try {
            String verifiedUuid = session.uuid;
            String verifiedUsername = session.username;

            achievementRepo.deleteByPlayerUuid(verifiedUuid);
            achievementRepo.flush();

            for (PlayerAchievementDto.AchievementState achievement : request.getAchievements()) {
                achievementRepo.save(new PlayerAchievement(
                        verifiedUuid,
                        verifiedUsername,
                        achievement.getId().trim(),
                        achievement.getUnlocked(),
                        achievement.getCurrent() == null ? 0 : achievement.getCurrent(),
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
                achievements.stream().map(this::toDto).toList()
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
                achievement.isUnlocked(),
                achievement.getCurrentProgress()
        );
    }

    private String normalizeUuid(String playerUuid) {
        if (playerUuid == null) return null;
        String normalizedUuid = playerUuid.replace("-", "").toLowerCase();
        return normalizedUuid.matches("[0-9a-f]{32}") ? normalizedUuid : null;
    }

    private String validateUploadRequest(PlayerAchievementDto.UploadRequest request) {
        if (request == null) {
            return "Request body must not be empty";
        }
        if (request.getSchemaVersion() == null) {
            return "schemaVersion must be provided";
        }
        if (request.getSchemaVersion() != 1) {
            return "Unsupported achievement schemaVersion: " + request.getSchemaVersion();
        }
        if (request.getAchievements() == null) {
            return "achievements must be provided";
        }
        if (request.getAchievements().isEmpty()) {
            return "No achievements provided";
        }
        if (request.getModVersion() != null && request.getModVersion().length() > 50) {
            return "modVersion must be at most 50 characters";
        }
        return null;
    }

    private Map<String, String> createResponse(String status, String message) {
        Map<String, String> response = new HashMap<>();
        response.put("status", status);
        response.put("message", message);
        return response;
    }
}

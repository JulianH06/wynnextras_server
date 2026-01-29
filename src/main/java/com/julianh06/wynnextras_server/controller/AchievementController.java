package com.julianh06.wynnextras_server.controller;

import com.julianh06.wynnextras_server.dto.AchievementSyncDto;
import com.julianh06.wynnextras_server.entity.AchievementPlayer;
import com.julianh06.wynnextras_server.entity.PlayerAchievement;
import com.julianh06.wynnextras_server.repository.AchievementPlayerRepository;
import com.julianh06.wynnextras_server.repository.PlayerAchievementRepository;
import com.julianh06.wynnextras_server.service.MojangAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for WynnExtras Achievement System.
 *
 * Endpoints:
 * - POST /achievements - Sync player's achievements (authenticated)
 * - GET /achievements?playerUuid=X - Get player's achievements (public)
 * - GET /achievements/player?playerUuid=X - Get player's achievement summary (public)
 * - GET /achievements/leaderboard - Get top players by points (public)
 * - GET /achievements/players - Get list of all players with achievements (public)
 */
@RestController
@RequestMapping("/achievements")
public class AchievementController {
    private static final Logger logger = LoggerFactory.getLogger(AchievementController.class);

    @Autowired
    private PlayerAchievementRepository achievementRepo;

    @Autowired
    private AchievementPlayerRepository playerRepo;

    @Autowired
    private MojangAuthService mojangAuth;

    /**
     * Sync player's achievements
     * POST /achievements
     *
     * Headers: Username, Server-ID (Mojang auth)
     * Body: { "modVersion": "...", "achievements": [...] }
     */
    @PostMapping
    @Transactional
    public ResponseEntity<?> syncAchievements(
            @RequestBody AchievementSyncDto syncDto,
            @RequestHeader(value = "Username", required = false) String username,
            @RequestHeader(value = "Server-ID", required = false) String serverId) {

        // Validate authentication
        boolean hasMojangAuth = username != null && !username.trim().isEmpty()
                             && serverId != null && !serverId.trim().isEmpty();

        if (!hasMojangAuth) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error",
                           "message", "Authentication required: provide Username + Server-ID headers"));
        }

        // Authenticate via Mojang
        MojangAuthService.AuthResult authResult = mojangAuth.verifyPlayer(username, serverId);
        if (!authResult.isSuccess()) {
            logger.warn("Mojang authentication failed for user {}: {}", username, authResult.getError());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "error", "message", authResult.getError()));
        }

        String playerUuid = authResult.getUuid();
        String playerName = authResult.getUsername();

        // Validate request
        if (syncDto.getModVersion() == null || syncDto.getModVersion().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", "modVersion is required"));
        }

        if (syncDto.getAchievements() == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", "achievements list is required"));
        }

        try {
            // Find or create player
            AchievementPlayer player = playerRepo.findByUuid(playerUuid)
                .orElse(new AchievementPlayer(playerUuid, playerName, syncDto.getModVersion()));

            player.setPlayerName(playerName); // Update in case username changed
            player.setModVersion(syncDto.getModVersion());
            player.setLastSyncedAt(Instant.now());

            // Counters for stats
            int unlockedCount = 0;
            int bronzeCount = 0;
            int silverCount = 0;
            int goldCount = 0;

            // Process each achievement
            for (AchievementSyncDto.AchievementDto dto : syncDto.getAchievements()) {
                if (dto.getId() == null || dto.getId().trim().isEmpty()) {
                    continue; // Skip invalid entries
                }

                // Find or create achievement record
                PlayerAchievement achievement = achievementRepo
                    .findByPlayerUuidAndAchievementId(playerUuid, dto.getId())
                    .orElse(new PlayerAchievement(playerUuid, dto.getId(), dto.getCategory()));

                // Update achievement data
                achievement.setCategory(dto.getCategory() != null ? dto.getCategory() : "MISC");
                achievement.setProgress(dto.getProgress());
                achievement.setUnlocked(dto.isUnlocked());
                achievement.setTier(dto.getTier());
                achievement.setUpdatedAt(Instant.now());

                // Handle unlock timestamp
                if (dto.isUnlocked() && dto.getUnlockedAt() != null) {
                    achievement.setUnlockedAt(Instant.ofEpochMilli(dto.getUnlockedAt()));
                } else if (dto.isUnlocked() && achievement.getUnlockedAt() == null) {
                    achievement.setUnlockedAt(Instant.now());
                }

                // Handle tier upgrade timestamp
                if (dto.getTier() != null && dto.getTierUpgradedAt() != null) {
                    achievement.setTierUpgradedAt(Instant.ofEpochMilli(dto.getTierUpgradedAt()));
                }

                achievementRepo.save(achievement);

                // Update counters
                if (dto.isUnlocked()) {
                    unlockedCount++;
                    if ("GOLD".equals(dto.getTier())) {
                        goldCount++;
                    } else if ("SILVER".equals(dto.getTier())) {
                        silverCount++;
                    } else if ("BRONZE".equals(dto.getTier())) {
                        bronzeCount++;
                    }
                }
            }

            // Update player stats
            player.recalculateStats(unlockedCount, bronzeCount, silverCount, goldCount);
            playerRepo.save(player);

            logger.info("Synced {} achievements for player {} ({})",
                syncDto.getAchievements().size(), playerName, playerUuid);

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Achievements synced",
                "stats", new SyncStats(unlockedCount, bronzeCount, silverCount, goldCount, player.getTotalPoints())
            ));
        } catch (Exception e) {
            logger.error("Error syncing achievements for player {}", playerName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", "Error syncing achievements"));
        }
    }

    /**
     * Get player's achievements
     * GET /achievements?playerUuid=X
     */
    @GetMapping
    public ResponseEntity<?> getPlayerAchievements(@RequestParam String playerUuid) {
        if (playerUuid == null || playerUuid.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", "playerUuid is required"));
        }

        // Normalize UUID
        String normalizedUuid = playerUuid.replace("-", "").toLowerCase();

        try {
            List<PlayerAchievement> achievements = achievementRepo.findByPlayerUuid(normalizedUuid);
            Optional<AchievementPlayer> player = playerRepo.findByUuid(normalizedUuid);

            if (player.isEmpty() && achievements.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Player not found"));
            }

            // Convert to response format
            List<AchievementResponse> achievementList = achievements.stream()
                .map(a -> new AchievementResponse(
                    a.getAchievementId(),
                    a.getCategory(),
                    a.getProgress(),
                    a.getTier(),
                    a.isUnlocked(),
                    a.getUnlockedAt() != null ? a.getUnlockedAt().toEpochMilli() : null,
                    a.getTierUpgradedAt() != null ? a.getTierUpgradedAt().toEpochMilli() : null
                ))
                .collect(Collectors.toList());

            PlayerSummary summary = player.map(p -> new PlayerSummary(
                p.getUuid(),
                p.getPlayerName(),
                p.getTotalPoints(),
                p.getUnlockedCount(),
                p.getGoldCount(),
                p.getSilverCount(),
                p.getBronzeCount(),
                p.getLastSyncedAt().toEpochMilli()
            )).orElse(null);

            return ResponseEntity.ok(Map.of(
                "player", summary,
                "achievements", achievementList
            ));
        } catch (Exception e) {
            logger.error("Error fetching achievements for player {}", playerUuid, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", "Error fetching achievements"));
        }
    }

    /**
     * Get player's achievement summary (without full achievement list)
     * GET /achievements/player?playerUuid=X
     */
    @GetMapping("/player")
    public ResponseEntity<?> getPlayerSummary(@RequestParam String playerUuid) {
        if (playerUuid == null || playerUuid.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", "playerUuid is required"));
        }

        String normalizedUuid = playerUuid.replace("-", "").toLowerCase();

        try {
            Optional<AchievementPlayer> player = playerRepo.findByUuid(normalizedUuid);

            if (player.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Player not found"));
            }

            AchievementPlayer p = player.get();
            return ResponseEntity.ok(new PlayerSummary(
                p.getUuid(),
                p.getPlayerName(),
                p.getTotalPoints(),
                p.getUnlockedCount(),
                p.getGoldCount(),
                p.getSilverCount(),
                p.getBronzeCount(),
                p.getLastSyncedAt().toEpochMilli()
            ));
        } catch (Exception e) {
            logger.error("Error fetching player summary for {}", playerUuid, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", "Error fetching player"));
        }
    }

    /**
     * Get achievement leaderboard
     * GET /achievements/leaderboard?limit=N
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "points") String sortBy) {

        if (limit < 1 || limit > 100) {
            limit = 50;
        }

        try {
            List<AchievementPlayer> players;

            switch (sortBy) {
                case "gold":
                    players = playerRepo.findTop100ByOrderByGoldCountDesc();
                    break;
                case "unlocked":
                    players = playerRepo.findTop100ByOrderByUnlockedCountDesc();
                    break;
                case "points":
                default:
                    players = playerRepo.findTop100ByOrderByTotalPointsDesc();
                    break;
            }

            // Limit results
            if (players.size() > limit) {
                players = players.subList(0, limit);
            }

            List<LeaderboardEntry> entries = players.stream()
                .map(p -> new LeaderboardEntry(
                    p.getUuid(),
                    p.getPlayerName(),
                    p.getTotalPoints(),
                    p.getUnlockedCount(),
                    p.getGoldCount(),
                    p.getSilverCount(),
                    p.getBronzeCount()
                ))
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "leaderboard", entries,
                "count", entries.size(),
                "sortBy", sortBy
            ));
        } catch (Exception e) {
            logger.error("Error fetching leaderboard", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", "Error fetching leaderboard"));
        }
    }

    /**
     * Get list of all players with achievements
     * GET /achievements/players
     */
    @GetMapping("/players")
    public ResponseEntity<?> getPlayers() {
        try {
            List<AchievementPlayer> players = playerRepo.findAllByOrderByTotalPointsDesc();

            List<PlayerListEntry> entries = players.stream()
                .map(p -> new PlayerListEntry(
                    p.getUuid(),
                    p.getPlayerName(),
                    p.getTotalPoints(),
                    p.getUnlockedCount(),
                    p.getLastSyncedAt().toEpochMilli()
                ))
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "players", entries,
                "count", entries.size()
            ));
        } catch (Exception e) {
            logger.error("Error fetching players", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", "Error fetching players"));
        }
    }

    // Response DTOs

    public static class SyncStats {
        public int unlocked;
        public int bronze;
        public int silver;
        public int gold;
        public int points;

        public SyncStats(int unlocked, int bronze, int silver, int gold, int points) {
            this.unlocked = unlocked;
            this.bronze = bronze;
            this.silver = silver;
            this.gold = gold;
            this.points = points;
        }
    }

    public static class AchievementResponse {
        public String id;
        public String category;
        public int progress;
        public String tier;
        public boolean unlocked;
        public Long unlockedAt;
        public Long tierUpgradedAt;

        public AchievementResponse(String id, String category, int progress, String tier,
                                   boolean unlocked, Long unlockedAt, Long tierUpgradedAt) {
            this.id = id;
            this.category = category;
            this.progress = progress;
            this.tier = tier;
            this.unlocked = unlocked;
            this.unlockedAt = unlockedAt;
            this.tierUpgradedAt = tierUpgradedAt;
        }
    }

    public static class PlayerSummary {
        public String uuid;
        public String playerName;
        public int totalPoints;
        public int unlockedCount;
        public int goldCount;
        public int silverCount;
        public int bronzeCount;
        public long lastSyncedAt;

        public PlayerSummary(String uuid, String playerName, int totalPoints, int unlockedCount,
                            int goldCount, int silverCount, int bronzeCount, long lastSyncedAt) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.totalPoints = totalPoints;
            this.unlockedCount = unlockedCount;
            this.goldCount = goldCount;
            this.silverCount = silverCount;
            this.bronzeCount = bronzeCount;
            this.lastSyncedAt = lastSyncedAt;
        }
    }

    public static class LeaderboardEntry {
        public String uuid;
        public String playerName;
        public int totalPoints;
        public int unlockedCount;
        public int goldCount;
        public int silverCount;
        public int bronzeCount;

        public LeaderboardEntry(String uuid, String playerName, int totalPoints, int unlockedCount,
                               int goldCount, int silverCount, int bronzeCount) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.totalPoints = totalPoints;
            this.unlockedCount = unlockedCount;
            this.goldCount = goldCount;
            this.silverCount = silverCount;
            this.bronzeCount = bronzeCount;
        }
    }

    public static class PlayerListEntry {
        public String uuid;
        public String playerName;
        public int totalPoints;
        public int unlockedCount;
        public long lastSyncedAt;

        public PlayerListEntry(String uuid, String playerName, int totalPoints,
                              int unlockedCount, long lastSyncedAt) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.totalPoints = totalPoints;
            this.unlockedCount = unlockedCount;
            this.lastSyncedAt = lastSyncedAt;
        }
    }

    // Helper Map class for response building
    private static class Map<K, V> extends HashMap<K, V> {
        public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
            Map<K, V> map = new Map<>();
            map.put(k1, v1);
            map.put(k2, v2);
            return map;
        }

        public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
            Map<K, V> map = new Map<>();
            map.put(k1, v1);
            map.put(k2, v2);
            map.put(k3, v3);
            return map;
        }

        public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
            Map<K, V> map = new Map<>();
            map.put(k1, v1);
            map.put(k2, v2);
            map.put(k3, v3);
            map.put(k4, v4);
            return map;
        }
    }
}

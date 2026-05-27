package com.julianh06.wynnextras_server.controller;

import com.julianh06.wynnextras_server.WynncraftService;
import com.julianh06.wynnextras_server.entity.WynncraftUsageSnapshot;
import com.julianh06.wynnextras_server.repository.*;
import com.julianh06.wynnextras_server.service.GuildStatsService;
import com.julianh06.wynnextras_server.service.VerifiedUserLoader;
import com.julianh06.wynnextras_server.service.WynncraftUsageStatsService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin endpoints for server management
 */
@RestController
@RequestMapping("/admin")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired private VerifiedUserLoader verifiedUserLoader;
    @Autowired private RaidLootPoolApprovedRepository raidApprovedRepo;
    @Autowired private RaidLootPoolSubmissionRepository raidSubmissionRepo;
    @Autowired private LootrunLootPoolApprovedRepository lootrunApprovedRepo;
    @Autowired private LootrunLootPoolSubmissionRepository lootrunSubmissionRepo;
    @Autowired private PersonalAspectRepository personalAspectRepo;
    @Autowired private GuildStatsService guildStatsService;
    @Autowired private WynncraftService wynncraftService;
    @Autowired private WynncraftUsageStatsService wynncraftUsageStatsService;

    /**
     * Reload verified users from file
     * POST /admin/reload-verified-users
     *
     * No authentication required for now - add if needed
     */
    @PostMapping("/reload-verified-users")
    public ResponseEntity<?> reloadVerifiedUsers() {
        try {
            logger.info("Admin triggered verified users reload");
            verifiedUserLoader.loadVerifiedUsers();

            long count = verifiedUserLoader.getVerifiedUserCount();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Verified users reloaded successfully");
            response.put("verifiedUserCount", count);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error reloading verified users", e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to reload verified users: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get verified users count
     * GET /admin/verified-users/count
     */
    @GetMapping("/verified-users/count")
    public ResponseEntity<?> getVerifiedUserCount() {
        long count = verifiedUserLoader.getVerifiedUserCount();

        Map<String, Object> response = new HashMap<>();
        response.put("verifiedUserCount", count);

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /admin/loot-pool/raid?raidType=NOTG - specific raid
     * DELETE /admin/loot-pool/raid - all raids
     */
    @DeleteMapping("/loot-pool/raid")
    @Transactional
    public ResponseEntity<?> wipeRaidLootPool(@RequestParam(required = false) String raidType) {
        if (raidType != null && !raidType.isBlank()) {
            long approvedDeleted = raidApprovedRepo.deleteByRaidType(raidType);
            long submissionsDeleted = raidSubmissionRepo.deleteByRaidType(raidType);
            logger.info("Admin wiped raid loot pool for {}: {} approved, {} submissions deleted",
                    raidType, approvedDeleted, submissionsDeleted);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Raid loot pool wiped for: " + raidType,
                    "approvedDeleted", approvedDeleted,
                    "submissionsDeleted", submissionsDeleted
            ));
        } else {
            long approvedDeleted = raidApprovedRepo.count();
            long submissionsDeleted = raidSubmissionRepo.count();
            raidApprovedRepo.deleteAll();
            raidSubmissionRepo.deleteAll();
            logger.info("Admin wiped ALL raid loot pools: {} approved, {} submissions deleted",
                    approvedDeleted, submissionsDeleted);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "All raid loot pools wiped",
                    "approvedDeleted", approvedDeleted,
                    "submissionsDeleted", submissionsDeleted
            ));
        }
    }

    @DeleteMapping("/loot-pool/lootrun")
    @Transactional
    public ResponseEntity<?> wipeLootrunLootPool(@RequestParam(required = false) String lootrunType) {
        if (lootrunType != null && !lootrunType.isBlank()) {
            long approvedDeleted = lootrunApprovedRepo.deleteByLootrunType(lootrunType);
            long submissionsDeleted = lootrunSubmissionRepo.deleteByLootrunType(lootrunType);
            logger.info("Admin wiped lootrun loot pool for {}: {} approved, {} submissions deleted",
                    lootrunType, approvedDeleted, submissionsDeleted);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Lootrun loot pool wiped for: " + lootrunType,
                    "approvedDeleted", approvedDeleted,
                    "submissionsDeleted", submissionsDeleted
            ));
        } else {
            long approvedDeleted = lootrunApprovedRepo.count();
            long submissionsDeleted = lootrunSubmissionRepo.count();
            lootrunApprovedRepo.deleteAll();
            lootrunSubmissionRepo.deleteAll();
            logger.info("Admin wiped ALL lootrun loot pools: {} approved, {} submissions deleted",
                    approvedDeleted, submissionsDeleted);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "All lootrun loot pools wiped",
                    "approvedDeleted", approvedDeleted,
                    "submissionsDeleted", submissionsDeleted
            ));
        }
    }

    @DeleteMapping("/aspects")
    @Transactional
    public ResponseEntity<?> wipeAspectByName(@RequestParam String aspectName) {
        if (aspectName == null || aspectName.isBlank()) {
            return ResponseEntity.badRequest().body("aspectName darf nicht leer sein");
        }
        int deleted = personalAspectRepo.deleteByAspectName(aspectName);
        logger.info("Admin wiped aspect '{}': {} entries deleted", aspectName, deleted);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Aspect gewiped: " + aspectName,
                "deleted", deleted
        ));
    }

    @GetMapping("/aspects/stale-preview")
    public ResponseEntity<?> previewStaleAspects() {
        try {
            return ResponseEntity.ok(buildStaleAspectPreview(wynncraftService.fetchCurrentAspectNames()));

        } catch (Exception e) {
            logger.error("Error previewing stale aspects", e);
            return ResponseEntity.status(502).body(Map.of(
                    "status", "error",
                    "message", "Failed to fetch current Wynncraft aspects: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/aspects/stale")
    @Transactional
    public ResponseEntity<?> wipeStaleAspects(@RequestParam(defaultValue = "false") boolean confirm) {
        if (!confirm) {
            return ResponseEntity.badRequest().body("confirm=true erforderlich");
        }

        try {
            Set<String> currentAspectNames = wynncraftService.fetchCurrentAspectNames();
            Map<String, Object> preview = buildStaleAspectPreview(currentAspectNames);
            int deleted = personalAspectRepo.deleteByAspectNameNotIn(currentAspectNames);

            logger.info("Admin wiped stale aspects: {} entries deleted", deleted);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Stale Aspects gewiped",
                    "currentAspectCount", preview.get("currentAspectCount"),
                    "staleAspectCount", preview.get("staleAspectCount"),
                    "expectedDeleted", preview.get("totalStaleRows"),
                    "deleted", deleted,
                    "staleAspects", preview.get("staleAspects")
            ));

        } catch (Exception e) {
            logger.error("Error wiping stale aspects", e);
            return ResponseEntity.status(502).body(Map.of(
                    "status", "error",
                    "message", "Failed to fetch current Wynncraft aspects: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/aspects/player")
    @Transactional
    public ResponseEntity<?> wipePlayerAspects(@RequestParam String playerUuid) {
        String normalized = playerUuid.replace("-", "").toLowerCase();
        if (!normalized.matches("[0-9a-f]{32}")) {
            return ResponseEntity.badRequest().body("Ungültige UUID");
        }
        personalAspectRepo.deleteByPlayerUuid(normalized);
        logger.info("Admin wiped all aspects for player UUID: {}", normalized);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Alle Aspects gewiped für UUID: " + normalized
        ));
    }

    private Map<String, Object> buildStaleAspectPreview(Set<String> currentAspectNames) {
        if (currentAspectNames == null || currentAspectNames.isEmpty()) {
            throw new IllegalStateException("Current aspect list is empty");
        }

        List<Map<String, Object>> staleAspects = new ArrayList<>();
        long totalStaleRows = 0;

        for (PersonalAspectRepository.AspectNameCount aspect : personalAspectRepo.findAspectNameCounts()) {
            if (!currentAspectNames.contains(aspect.getAspectName())) {
                staleAspects.add(Map.of(
                        "aspectName", aspect.getAspectName(),
                        "entryCount", aspect.getEntryCount()
                ));
                totalStaleRows += aspect.getEntryCount();
            }
        }

        return Map.of(
                "status", "success",
                "currentAspectCount", currentAspectNames.size(),
                "staleAspectCount", staleAspects.size(),
                "totalStaleRows", totalStaleRows,
                "staleAspects", staleAspects
        );
    }

    @GetMapping("/guild-lookup")
    public ResponseEntity<?> guildLookup(@RequestParam String tag) {
        try {
            return ResponseEntity.ok(guildStatsService.lookupGuild(tag).toResponseMap());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(502).body(Map.of("error", "Request interrupted"));
        } catch (GuildStatsService.GuildLookupException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching guild data for tag: {}", tag, e);
            return ResponseEntity.status(502).body(Map.of("error", "Error: " + e.getMessage()));
        }
    }

    @PostMapping("/wynncraft-usage/snapshot")
    public ResponseEntity<?> captureWynncraftUsageSnapshot() {
        Instant snapshotInstant = Instant.now();
        LocalDate snapshotDate = LocalDate.ofInstant(snapshotInstant, ZoneOffset.UTC);

        try {
            WynncraftUsageStatsService.CapturedOnlinePlayerSample sample =
                    wynncraftUsageStatsService.captureOnlinePlayerSample(snapshotInstant);
            WynncraftUsageSnapshot snapshot = wynncraftUsageStatsService.captureDailyUsageSnapshot(
                    snapshotDate,
                    snapshotInstant,
                    (long) sample.totalOnlinePlayers());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "snapshotDate", snapshot.getSnapshotDate().toString(),
                    "capturedAt", snapshot.getCapturedAt().toString(),
                    "visiblePlayersSampled", sample.visiblePlayers(),
                    "totalOnlinePlayers", snapshot.getTotalOnlinePlayers(),
                    "uniquePlayers", snapshot.getUniquePlayers(),
                    "wynnExtrasUsers", snapshot.getWynnExtrasUsers(),
                    "usagePercent", snapshot.getUsagePercent(),
                    "sampleCount", snapshot.getSampleCount()
            ));
        } catch (Exception e) {
            logger.error("Error capturing manual Wynncraft usage snapshot", e);
            return ResponseEntity.status(502).body(Map.of(
                    "status", "error",
                    "message", "Failed to capture Wynncraft usage snapshot: " + e.getMessage()
            ));
        }
    }
}

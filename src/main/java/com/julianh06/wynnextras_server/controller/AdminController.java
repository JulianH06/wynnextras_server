package com.julianh06.wynnextras_server.controller;

import com.julianh06.wynnextras_server.repository.*;
import com.julianh06.wynnextras_server.service.VerifiedUserLoader;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
}

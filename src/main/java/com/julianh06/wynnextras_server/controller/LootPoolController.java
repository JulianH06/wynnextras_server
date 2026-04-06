package com.julianh06.wynnextras_server.controller;

import com.julianh06.wynnextras_server.dto.LootPoolSubmissionDto;
import com.julianh06.wynnextras_server.service.LootPoolService;
import com.julianh06.wynnextras_server.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/raid")
public class LootPoolController {
    private static final Logger logger = LoggerFactory.getLogger(LootPoolController.class);

    @Autowired
    private LootPoolService lootPoolService;

    @Autowired
    private AuthService mojangAuth;

    /**
     * Submit a loot pool for a raid
     * POST /raid/loot-pool
     *
     * Supports dual authentication:
     * 1. Mojang Sessionserver (new mod): Headers: Username, Server-ID
     * 2. Wynncraft API Key (future/compatibility): Headers: Wynncraft-Api-Key, Player-UUID
     *
     * Body: { "raidType": "...", "aspects": [{"name": "...", "rarity": "...", "requiredClass": "..."}] }
     */
    @PostMapping("/loot-pool")
    public ResponseEntity<?> submitLootPool(
            @RequestBody LootPoolSubmissionDto submission,
            @RequestHeader(value = "Authorization", required = false) String token) {

        // Validate session token
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status","error","message","Missing session token"));
        }

        AuthService.SessionData session = AuthService.validateSession(token);

        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status","error","message","Session expired or invalid"));
        }

        String verifiedUsername = session.username;

        // Validate raid type from body
        String raidType = submission.getRaidType();
        if (raidType == null || raidType.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Missing raidType in request body");
        }

        if (!isValidRaidType(raidType)) {
            logger.warn("Invalid raid type: {}", raidType);
            return ResponseEntity.badRequest().body("Invalid raid type");
        }

        // Submit loot pool
        try {
            LootPoolSubmissionDto approved = lootPoolService.submitLootPool(
                raidType,
                submission.getAspects(),
                verifiedUsername
            );

            if (approved != null) {
                logger.info("Loot pool for {} was approved (submitted by {})", raidType, verifiedUsername);
                return ResponseEntity.ok().body(Map.of(
                    "status", "approved",
                    "message", "Loot pool approved for " + raidType,
                    "lootPool", approved
                ));
            } else {
                logger.info("Loot pool for {} submitted by {} but not yet approved", raidType, verifiedUsername);
                return ResponseEntity.ok().body(Map.of(
                    "status", "submitted",
                    "message", "Loot pool submitted. Waiting for more confirmations."
                ));
            }
        } catch (Exception e) {
            logger.error("Error submitting loot pool", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing submission");
        }
    }

    /**
     * Get the approved loot pool for a raid
     * GET /raid/loot-pool?raidType=...
     */
    @GetMapping("/loot-pool")
    public ResponseEntity<?> getLootPool(@RequestParam String raidType) {
        if (raidType == null || raidType.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Missing raidType parameter");
        }

        if (!isValidRaidType(raidType)) {
            return ResponseEntity.badRequest().body("Invalid raid type");
        }

        LootPoolSubmissionDto lootPool = lootPoolService.getApprovedLootPool(raidType);

        if (lootPool != null) {
            return ResponseEntity.ok(lootPool);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No approved loot pool for " + raidType);
        }
    }

    private boolean isValidRaidType(String raidType) {
        // Accept both short codes and full names
        return raidType.equals("NOTG") || raidType.equals("NOL") ||
            raidType.equals("TCC") || raidType.equals("TNA") || raidType.equals("TWP") ||
            raidType.equals("Nest of the Grootslangs") ||
            raidType.equals("Orphion's Nexus of Light") ||
            raidType.equals("The Canyon Colossus") ||
            raidType.equals("The Nameless Anomaly") ||
            raidType.equals("The Wartorn Palace");
    }

    private static class Map<K, V> extends java.util.HashMap<K, V> {
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
    }
}

package com.julianh06.wynnextras_server.controller;

import com.julianh06.wynnextras_server.dto.LootPoolSubmissionDto;
import com.julianh06.wynnextras_server.service.LootPoolService;
import com.julianh06.wynnextras_server.service.MojangAuthService;
import com.julianh06.wynnextras_server.service.WynnAPIKeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lootpool")
public class LootPoolController {
    private static final Logger logger = LoggerFactory.getLogger(LootPoolController.class);

    @Autowired
    private LootPoolService lootPoolService;

    @Autowired
    private MojangAuthService mojangAuth;

    @Autowired
    private WynnAPIKeyService wynnApiKeyService;

    /**
     * Submit a loot pool for a raid
     * POST /lootpool/{raidType}
     *
     * Supports dual authentication:
     * 1. Mojang Sessionserver (new mod): Headers: Username, Server-ID
     * 2. Wynncraft API Key (future/compatibility): Headers: Wynncraft-Api-Key, Player-UUID
     *
     * Body: { "aspects": [{"name": "...", "rarity": "...", "requiredClass": "..."}] }
     */
    @PostMapping("/{raidType}")
    public ResponseEntity<?> submitLootPool(
            @PathVariable String raidType,
            @RequestBody LootPoolSubmissionDto submission,
            @RequestHeader(value = "Username", required = false) String username,
            @RequestHeader(value = "Server-ID", required = false) String serverId,
            @RequestHeader(value = "Wynncraft-Api-Key", required = false) String wynnApiKey,
            @RequestHeader(value = "Player-UUID", required = false) String playerUuid) {

        // Validate raid type
        if (!isValidRaidType(raidType)) {
            logger.warn("Invalid raid type: {}", raidType);
            return ResponseEntity.badRequest().body("Invalid raid type. Must be NOTG, NOL, TCC, or TNA");
        }

        String verifiedUsername;

        // Determine authentication method
        boolean hasMojangAuth = username != null && !username.trim().isEmpty()
                             && serverId != null && !serverId.trim().isEmpty();
        boolean hasWynnAuth = wynnApiKey != null && !wynnApiKey.trim().isEmpty()
                           && playerUuid != null && !playerUuid.trim().isEmpty();

        if (hasMojangAuth) {
            // Mojang Sessionserver authentication
            MojangAuthService.AuthResult authResult = mojangAuth.verifyPlayer(username, serverId);
            if (!authResult.isSuccess()) {
                logger.warn("Mojang authentication failed for user {}: {}", username, authResult.getError());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", authResult.getError()));
            }
            verifiedUsername = authResult.getUsername();

        } else if (hasWynnAuth) {
            // Wynncraft API Key authentication
            WynnAPIKeyService.AuthResult authResult = wynnApiKeyService.validateApiKey(wynnApiKey, playerUuid);
            if (!authResult.isSuccess()) {
                logger.warn("Wynncraft API key validation failed: {}", authResult.getError());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", authResult.getError()));
            }
            verifiedUsername = authResult.getUsername();

        } else {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error",
                           "message", "Authentication required: provide either (Username + Server-ID) or (Wynncraft-Api-Key + Player-UUID)"));
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
     * GET /lootpool/{raidType}
     */
    @GetMapping("/{raidType}")
    public ResponseEntity<?> getLootPool(@PathVariable String raidType) {
        if (!isValidRaidType(raidType)) {
            return ResponseEntity.badRequest().body("Invalid raid type. Must be NOTG, NOL, TCC, or TNA");
        }

        LootPoolSubmissionDto lootPool = lootPoolService.getApprovedLootPool(raidType);

        if (lootPool != null) {
            return ResponseEntity.ok(lootPool);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No approved loot pool for " + raidType);
        }
    }

    private boolean isValidRaidType(String raidType) {
        return raidType.equals("NOTG") || raidType.equals("NOL") ||
               raidType.equals("TCC") || raidType.equals("TNA");
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

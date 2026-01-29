package com.julianh06.wynnextras_server.controller;

import com.julianh06.wynnextras_server.dto.LootrunLootPoolSubmissionDto;
import com.julianh06.wynnextras_server.service.LootrunLootPoolService;
import com.julianh06.wynnextras_server.service.MojangAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("/lootrun")
public class LootrunLootPoolController {
    private static final Logger logger = LoggerFactory.getLogger(LootrunLootPoolController.class);

    @Autowired
    private LootrunLootPoolService lootrunLootPoolService;

    @Autowired
    private MojangAuthService mojangAuth;

    /**
     * Submit a loot pool for a lootrun
     * POST /lootrun/loot-pool
     *
     * Headers: Username, Server-ID (Mojang auth)
     * Body: { "lootrunType": "...", "items": [{"name": "...", "rarity": "...", "type": "..."}] }
     */
    @PostMapping("/loot-pool")
    public ResponseEntity<?> submitLootPool(
            @RequestBody LootrunLootPoolSubmissionDto submission,
            @RequestHeader(value = "Username", required = false) String username,
            @RequestHeader(value = "Server-ID", required = false) String serverId) {

        // Validate lootrun type from body
        String lootrunType = submission.getLootrunType();
        if (lootrunType == null || lootrunType.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Missing lootrunType in request body");
        }

        if (!isValidLootrunType(lootrunType)) {
            logger.warn("Invalid lootrun type: {}", lootrunType);
            return ResponseEntity.badRequest().body("Invalid lootrun type");
        }

        // Determine authentication method
        boolean hasMojangAuth = username != null && !username.trim().isEmpty()
                             && serverId != null && !serverId.trim().isEmpty();

        if (!hasMojangAuth) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error",
                           "message", "Authentication required: provide Username + Server-ID headers"));
        }

        // Mojang Sessionserver authentication
        MojangAuthService.AuthResult authResult = mojangAuth.verifyPlayer(username, serverId);
        if (!authResult.isSuccess()) {
            logger.warn("Mojang authentication failed for user {}: {}", username, authResult.getError());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "error", "message", authResult.getError()));
        }
        String verifiedUsername = authResult.getUsername();

        // Submit loot pool
        try {
            LootrunLootPoolSubmissionDto approved = lootrunLootPoolService.submitLootPool(
                lootrunType,
                submission.getItems(),
                verifiedUsername
            );

            if (approved != null) {
                logger.info("Loot pool for {} was approved (submitted by {})", lootrunType, verifiedUsername);
                return ResponseEntity.ok().body(Map.of(
                    "status", "approved",
                    "message", "Loot pool approved for " + lootrunType,
                    "lootPool", approved
                ));
            } else {
                logger.info("Loot pool for {} submitted by {} but not yet approved", lootrunType, verifiedUsername);
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
     * Get the approved loot pool for a lootrun
     * GET /lootrun/loot-pool?lootrunType=...
     */
    @GetMapping("/loot-pool")
    public ResponseEntity<?> getLootPool(@RequestParam String lootrunType) {
        if (lootrunType == null || lootrunType.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Missing lootrunType parameter");
        }

        if (!isValidLootrunType(lootrunType)) {
            return ResponseEntity.badRequest().body("Invalid lootrun type");
        }

        LootrunLootPoolSubmissionDto lootPool = lootrunLootPoolService.getApprovedLootPool(lootrunType);

        if (lootPool != null) {
            return ResponseEntity.ok(lootPool);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No approved loot pool for " + lootrunType);
        }
    }

    private boolean isValidLootrunType(String lootrunType) {
        // Accept both short codes and full names
        return lootrunType.equals("SE") || lootrunType.equals("SI") ||
               lootrunType.equals("MH") || lootrunType.equals("CORK") ||
               lootrunType.equals("COTL") ||
               lootrunType.equals("Silent Expanse") ||
               lootrunType.equals("Sky Islands") ||
               lootrunType.equals("Molten Heights") ||
               lootrunType.equals("Corkus") ||
               lootrunType.equals("Canyon of the Lost");
    }

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
    }
}

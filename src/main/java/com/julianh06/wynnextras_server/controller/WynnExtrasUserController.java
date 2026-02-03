package com.julianh06.wynnextras_server.controller;

import com.julianh06.wynnextras_server.entity.WynnExtrasUser;
import com.julianh06.wynnextras_server.repository.WynnExtrasUserRepository;
import com.julianh06.wynnextras_server.service.MojangAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Controller for WynnExtras user badge (⭐) system.
 *
 * Endpoints:
 * - POST /wynnextras-users/heartbeat - Client heartbeat to register activity
 * - GET /wynnextras-users/active - Get list of active user UUIDs for badge display
 */
@RestController
@RequestMapping("/wynnextras-users")
public class WynnExtrasUserController {
    private static final Logger logger = LoggerFactory.getLogger(WynnExtrasUserController.class);
    private static final Duration ACTIVE_THRESHOLD = Duration.ofDays(7);

    @Autowired
    private WynnExtrasUserRepository userRepository;

    @Autowired
    private MojangAuthService mojangAuth;

    /**
     * Client heartbeat - registers or updates user activity
     * POST /wynnextras-users/heartbeat
     *
     * Headers: Username, Server-ID (Mojang auth)
     * Body: { "modVersion": "0.12.1" }
     *
     * Called on game launch and periodically (every 600 seconds)
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<?> heartbeat(
            @RequestBody HeartbeatRequest request,
            @RequestHeader(value = "Username", required = false) String username,
            @RequestHeader(value = "Server-ID", required = false) String serverId) {

        // Validate authentication headers
        boolean hasMojangAuth = username != null && !username.trim().isEmpty()
                             && serverId != null && !serverId.trim().isEmpty();

        if (!hasMojangAuth) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error",
                           "message", "Authentication required: provide Username + Server-ID headers"));
        }

        // Validate mod version
        if (request.getModVersion() == null || request.getModVersion().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error",
                           "message", "modVersion is required in request body"));
        }

        // Authenticate via Mojang
        MojangAuthService.AuthResult authResult = mojangAuth.verifyPlayer(username, serverId);
        if (!authResult.isSuccess()) {
            logger.warn("Mojang authentication failed for user {}: {}", username, authResult.getError());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "error", "message", authResult.getError()));
        }

        String verifiedUuid = authResult.getUuid();
        String verifiedUsername = authResult.getUsername();
        String modVersion = request.getModVersion().trim();

        try {
            // Find existing user or create new one
            Optional<WynnExtrasUser> existingUser = userRepository.findByUuid(verifiedUuid);

            if (existingUser.isPresent()) {
                // Update existing user
                WynnExtrasUser user = existingUser.get();
                user.setUsername(verifiedUsername); // Update username in case it changed
                user.setModVersion(modVersion);
                user.setLastSeen(Instant.now());
                userRepository.save(user);
                logger.debug("Updated heartbeat for user {} ({})", verifiedUsername, verifiedUuid);
            } else {
                // Create new user
                WynnExtrasUser user = new WynnExtrasUser(verifiedUuid, verifiedUsername, modVersion);
                userRepository.save(user);
                logger.info("Registered new WynnExtras user: {} ({})", verifiedUsername, verifiedUuid);
            }

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Heartbeat recorded"
            ));
        } catch (Exception e) {
            logger.error("Error processing heartbeat for user {}", verifiedUsername, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", "Error processing heartbeat"));
        }
    }

    /**
     * Get list of active WynnExtras user UUIDs
     * GET /wynnextras-users/active
     *
     * Returns UUIDs of users active within the last 7 days
     * Used by clients to display star badges on players using the mod
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveUsers() {
        try {
            Instant cutoff = Instant.now().minus(ACTIVE_THRESHOLD);
            List<String> activeUuids = userRepository.findActiveUuidsSince(cutoff);

            return ResponseEntity.ok(Map.of(
                "uuids", activeUuids,
                "count", activeUuids.size()
            ));
        } catch (Exception e) {
            logger.error("Error fetching active users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", "Error fetching active users"));
        }
    }

    /**
     * Get detailed info about active users (admin endpoint)
     * GET /wynnextras-users/active/details
     */
    @GetMapping("/active/details")
    public ResponseEntity<?> getActiveUsersDetails() {
        try {
            Instant cutoff = Instant.now().minus(ACTIVE_THRESHOLD);
            List<WynnExtrasUser> activeUsers = userRepository.findActiveUsersSince(cutoff);

            List<UserInfo> userInfos = activeUsers.stream()
                .map(u -> new UserInfo(u.getUuid(), u.getUsername(), u.getModVersion(), u.getLastSeen().toEpochMilli()))
                .toList();

            return ResponseEntity.ok(Map.of(
                "users", userInfos,
                "count", userInfos.size()
            ));
        } catch (Exception e) {
            logger.error("Error fetching active user details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", "Error fetching active users"));
        }
    }

    /**
     * Get statistics about WynnExtras user base
     * GET /wynnextras-users/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            Instant cutoff = Instant.now().minus(ACTIVE_THRESHOLD);
            long activeCount = userRepository.countActiveUsersSince(cutoff);
            long totalCount = userRepository.count();

            return ResponseEntity.ok(Map.of(
                "activeUsers", activeCount,
                "totalUsers", totalCount,
                "activeThresholdDays", ACTIVE_THRESHOLD.toDays()
            ));
        } catch (Exception e) {
            logger.error("Error fetching user stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", "Error fetching stats"));
        }
    }

    // Request/Response DTOs

    public static class HeartbeatRequest {
        private String modVersion;

        public String getModVersion() { return modVersion; }
        public void setModVersion(String modVersion) { this.modVersion = modVersion; }
    }

    public static class UserInfo {
        private String uuid;
        private String username;
        private String modVersion;
        private long lastSeen;

        public UserInfo(String uuid, String username, String modVersion, long lastSeen) {
            this.uuid = uuid;
            this.username = username;
            this.modVersion = modVersion;
            this.lastSeen = lastSeen;
        }

        public String getUuid() { return uuid; }
        public String getUsername() { return username; }
        public String getModVersion() { return modVersion; }
        public long getLastSeen() { return lastSeen; }
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
    }
}

package com.julianh06.wynnextras_server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for authenticating players via Mojang's sessionserver
 * Uses the standard Minecraft authentication flow without exposing session IDs
 */
@Service
public class MojangAuthService {
    private static final Logger logger = LoggerFactory.getLogger(MojangAuthService.class);
    private static final String MOJANG_SESSION_SERVER = "https://sessionserver.mojang.com/session/minecraft/hasJoined";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Cache successful verifications - allows same serverId to be reused within window
    // This is necessary because the client caches auth and sends multiple requests with the same serverId
    private final Map<String, CachedAuth> authCache = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_MS = 20000; // 20 seconds (slightly longer than client's 15s cache)

    /**
     * Cached authentication result
     */
    private static class CachedAuth {
        final String uuid;
        final String username;
        final long timestamp;

        CachedAuth(String uuid, String username) {
            this.uuid = uuid;
            this.username = username;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }

    /**
     * Verify a player's authentication with Mojang
     *
     * @param username The player's username
     * @param serverId The shared secret used in the authentication handshake
     * @return AuthResult with verified UUID and username, or error
     */
    public AuthResult verifyPlayer(String username, String serverId) {
        // Check if we have a cached verification for this serverId
        CachedAuth cached = authCache.get(serverId);
        if (cached != null && !cached.isExpired()) {
            // Verify the username matches (security check)
            if (cached.username.equalsIgnoreCase(username)) {
                logger.debug("Using cached auth for {} (serverId: {})", username, serverId.substring(0, 8) + "...");
                return AuthResult.success(cached.uuid, cached.username);
            } else {
                // Different username with same serverId - suspicious
                logger.warn("ServerId reuse attempt with different username: cached={}, requested={}", cached.username, username);
                return AuthResult.error("Authentication token mismatch");
            }
        }

        // Clean up expired entries if cache is getting large
        cleanupExpiredCache();

        try {
            // Call Mojang's sessionserver to verify the player
            String url = MOJANG_SESSION_SERVER +
                "?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8) +
                "&serverId=" + URLEncoder.encode(serverId, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parse response
                JsonNode json = objectMapper.readTree(response.body());
                String verifiedUuid = json.get("id").asText();
                String verifiedUsername = json.get("name").asText();

                // Normalize UUID (remove dashes if present, lowercase)
                verifiedUuid = verifiedUuid.replace("-", "").toLowerCase();

                // Cache the successful verification
                authCache.put(serverId, new CachedAuth(verifiedUuid, verifiedUsername));

                logger.info("Successfully authenticated player {} (UUID: {})", verifiedUsername, verifiedUuid);
                return AuthResult.success(verifiedUuid, verifiedUsername);
            } else if (response.statusCode() == 204) {
                // No Content - authentication failed
                logger.warn("Authentication failed for username {}: Invalid session", username);
                return AuthResult.error("Authentication failed - invalid session");
            } else {
                logger.error("Mojang sessionserver returned unexpected status: {}", response.statusCode());
                return AuthResult.error("Authentication service error");
            }
        } catch (Exception e) {
            logger.error("Error verifying player with Mojang", e);
            return AuthResult.error("Authentication failed - " + e.getMessage());
        }
    }

    /**
     * Clean up expired cache entries to prevent memory leak
     */
    private void cleanupExpiredCache() {
        if (authCache.size() > 500) {
            authCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
    }

    /**
     * Result of authentication attempt
     */
    public static class AuthResult {
        private final boolean success;
        private final String uuid;
        private final String username;
        private final String error;

        private AuthResult(boolean success, String uuid, String username, String error) {
            this.success = success;
            this.uuid = uuid;
            this.username = username;
            this.error = error;
        }

        public static AuthResult success(String uuid, String username) {
            return new AuthResult(true, uuid, username, null);
        }

        public static AuthResult error(String error) {
            return new AuthResult(false, null, null, error);
        }

        public boolean isSuccess() { return success; }
        public String getUuid() { return uuid; }
        public String getUsername() { return username; }
        public String getError() { return error; }
    }
}

package com.julianh06.wynnextras_server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for validating Wynncraft API keys
 * Used for backward compatibility with old mod versions
 */
@Service
public class WynnAPIKeyService {
    private static final Logger logger = LoggerFactory.getLogger(WynnAPIKeyService.class);
    private static final String WYNNCRAFT_API_BASE = "https://api.wynncraft.com/v3/player/";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Cache validated API keys (UUID -> last validation time)
    private final Map<String, Long> validatedKeys = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_MS = 300000; // 5 minutes

    /**
     * Validate a Wynncraft API key and verify it belongs to the claimed player
     *
     * @param apiKey The Wynncraft API key
     * @param claimedUuid The UUID the player claims to own
     * @return AuthResult with verified UUID and username, or error
     */
    public AuthResult validateApiKey(String apiKey, String claimedUuid) {
        // Normalize UUID
        String normalizedUuid = claimedUuid.replace("-", "").toLowerCase();

        // Check cache
        String cacheKey = apiKey + ":" + normalizedUuid;
        Long lastValidated = validatedKeys.get(cacheKey);
        if (lastValidated != null && System.currentTimeMillis() - lastValidated < CACHE_EXPIRY_MS) {
            logger.debug("Using cached API key validation for UUID: {}", normalizedUuid);
            // We don't cache the username, so we need to fetch it
            return fetchPlayerData(normalizedUuid, apiKey);
        }

        // Validate with Wynncraft API
        return fetchPlayerData(normalizedUuid, apiKey);
    }

    /**
     * Fetch player data from Wynncraft API with the provided API key
     */
    private AuthResult fetchPlayerData(String uuid, String apiKey) {
        try {
            // Add dashes to UUID for Wynncraft API
            String formattedUuid = formatUUID(uuid);

            String url = WYNNCRAFT_API_BASE + formattedUuid;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parse response to get username
                JsonNode json = objectMapper.readTree(response.body());
                String username = json.get("username").asText();

                // Normalize UUID (remove dashes if present)
                String normalizedUuid = uuid.replace("-", "").toLowerCase();

                // Cache validation
                String cacheKey = apiKey + ":" + normalizedUuid;
                validatedKeys.put(cacheKey, System.currentTimeMillis());
                cleanupExpiredCache();

                logger.info("Successfully validated Wynncraft API key for player {} (UUID: {})", username, normalizedUuid);
                return AuthResult.success(normalizedUuid, username);

            } else if (response.statusCode() == 401) {
                logger.warn("Invalid Wynncraft API key");
                return AuthResult.error("Invalid Wynncraft API key");

            } else if (response.statusCode() == 403) {
                logger.warn("Wynncraft API key does not belong to claimed UUID");
                return AuthResult.error("API key does not belong to this account");

            } else if (response.statusCode() == 404) {
                logger.warn("Player UUID not found on Wynncraft: {}", uuid);
                return AuthResult.error("Player not found on Wynncraft");

            } else {
                logger.error("Wynncraft API returned unexpected status: {}", response.statusCode());
                return AuthResult.error("Wynncraft API error");
            }

        } catch (Exception e) {
            logger.error("Error validating Wynncraft API key", e);
            return AuthResult.error("Failed to validate API key - " + e.getMessage());
        }
    }

    /**
     * Format UUID with dashes for Wynncraft API
     */
    private String formatUUID(String rawUUID) {
        // Remove dashes if present
        String cleaned = rawUUID.replace("-", "");

        // Add dashes: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
        return cleaned.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5"
        );
    }

    /**
     * Clean up expired cache entries to prevent memory leak
     */
    private void cleanupExpiredCache() {
        if (validatedKeys.size() > 1000) {
            long now = System.currentTimeMillis();
            validatedKeys.entrySet().removeIf(entry ->
                now - entry.getValue() > CACHE_EXPIRY_MS);
        }
    }

    /**
     * Result of API key validation
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

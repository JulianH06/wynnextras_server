package com.julianh06.wynnextras_server.controller;

import com.julianh06.wynnextras_server.dto.PersonalAspectDto;
import com.julianh06.wynnextras_server.entity.PersonalAspect;
import com.julianh06.wynnextras_server.repository.PersonalAspectRepository;
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
import java.util.stream.Collectors;

/**
 * Personal aspect tracking controller
 * Uses Mojang sessionserver authentication - no API keys or session IDs exposed
 */
@RestController
@RequestMapping("/user")
public class PersonalAspectController {
    private static final Logger logger = LoggerFactory.getLogger(PersonalAspectController.class);

    @Autowired
    private PersonalAspectRepository personalAspectRepo;

    @Autowired
    private MojangAuthService mojangAuth;

    /**
     * Upload personal aspects
     * POST /user
     * Headers:
     *   - Username (required) - Minecraft username
     *   - Server-ID (required) - Shared secret for Mojang verification
     * Body: { "playerName": "...", "modVersion": "...", "aspects": [...] }
     */
    @PostMapping
    @Transactional
    public ResponseEntity<?> uploadAspects(
            @RequestBody PersonalAspectDto.UploadRequest request,
            @RequestHeader("Username") String username,
            @RequestHeader("Server-ID") String serverId) {

        // Validate headers
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Missing Username header");
        }
        if (serverId == null || serverId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Missing Server-ID header");
        }

        // Authenticate with Mojang
        MojangAuthService.AuthResult authResult = mojangAuth.verifyPlayer(username, serverId);
        if (!authResult.isSuccess()) {
            logger.warn("Authentication failed for user {}: {}", username, authResult.getError());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createResponse("error", authResult.getError()));
        }

        String verifiedUuid = authResult.getUuid();
        String verifiedUsername = authResult.getUsername();

        // Validate request
        if (request.getAspects() == null || request.getAspects().isEmpty()) {
            return ResponseEntity.badRequest().body("No aspects provided");
        }

        try {
            // Save or update each aspect
            for (PersonalAspectDto.AspectData aspect : request.getAspects()) {
                var existing = personalAspectRepo.findByPlayerUuidAndAspectName(verifiedUuid, aspect.getName());

                if (existing.isPresent()) {
                    // Update existing
                    PersonalAspect pa = existing.get();
                    pa.setAmount(aspect.getAmount());
                    pa.setRarity(aspect.getRarity());
                    pa.setPlayerName(verifiedUsername);
                    pa.setModVersion(request.getModVersion());
                    pa.setUpdatedAt(Instant.now());
                    personalAspectRepo.save(pa);
                } else {
                    // Create new
                    PersonalAspect pa = new PersonalAspect(
                        verifiedUuid,
                        verifiedUsername,
                        aspect.getName(),
                        aspect.getRarity(),
                        aspect.getAmount(),
                        request.getModVersion()
                    );
                    personalAspectRepo.save(pa);
                }
            }

            logger.info("Saved {} aspects for verified player {} (UUID: {})",
                request.getAspects().size(), verifiedUsername, verifiedUuid);

            return ResponseEntity.ok().body(createResponse("success", "Aspects uploaded successfully"));

        } catch (Exception e) {
            logger.error("Error saving aspects", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createResponse("error", "Failed to save aspects"));
        }
    }

    /**
     * Get player's aspects
     * GET /user?playerUuid=xxx
     * No authentication required - anyone can view anyone's aspects
     */
    @GetMapping
    public ResponseEntity<?> getAspects(@RequestParam String playerUuid) {
        // Normalize UUID
        String normalizedUuid = playerUuid.replace("-", "").toLowerCase();
        if (!normalizedUuid.matches("[0-9a-f]{32}")) {
            return ResponseEntity.badRequest().body("Invalid UUID format");
        }

        List<PersonalAspect> aspects = personalAspectRepo.findByPlayerUuid(normalizedUuid);

        if (aspects.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("No aspects found for player");
        }

        // Build response
        PersonalAspect first = aspects.get(0);
        List<PersonalAspectDto.AspectData> aspectData = aspects.stream()
            .map(a -> new PersonalAspectDto.AspectData(a.getAspectName(), a.getRarity(), a.getAmount()))
            .collect(Collectors.toList());

        PersonalAspectDto.PlayerAspectsResponse response = new PersonalAspectDto.PlayerAspectsResponse(
            normalizedUuid,
            first.getPlayerName(),
            first.getModVersion(),
            first.getUpdatedAt().toEpochMilli(),
            aspectData
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get leaderboard of players with most max aspects
     * GET /user/leaderboard?limit=5
     * No authentication required - public leaderboard
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard(@RequestParam(defaultValue = "5") int limit) {
        if (limit < 1 || limit > 100) {
            return ResponseEntity.badRequest().body("Limit must be between 1 and 100");
        }

        try {
            List<Object[]> results = personalAspectRepo.findTopPlayersByMaxAspects(limit);

            List<PersonalAspectDto.LeaderboardEntry> leaderboard = results.stream()
                .map(row -> new PersonalAspectDto.LeaderboardEntry(
                    (String) row[0],  // playerUuid
                    (String) row[1],  // playerName
                    ((Number) row[2]).longValue()  // maxAspectCount
                ))
                .collect(Collectors.toList());

            return ResponseEntity.ok(leaderboard);

        } catch (Exception e) {
            logger.error("Error fetching leaderboard", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to fetch leaderboard");
        }
    }

    /**
     * Get all players who have aspects in the database
     * GET /user/list
     * No authentication required - public list
     * Returns players ordered by most recently updated first
     */
    @GetMapping("/list")
    public ResponseEntity<?> getAllPlayers() {
        try {
            List<Object[]> results = personalAspectRepo.findAllPlayersWithAspects();

            List<PersonalAspectDto.PlayerListEntry> players = results.stream()
                .map(row -> new PersonalAspectDto.PlayerListEntry(
                    (String) row[0],  // playerUuid
                    (String) row[1],  // playerName
                    (String) row[2],  // modVersion
                    ((java.time.Instant) row[3]).toEpochMilli(),  // lastUpdated
                    ((Number) row[4]).longValue()  // aspectCount
                ))
                .collect(Collectors.toList());

            return ResponseEntity.ok(players);

        } catch (Exception e) {
            logger.error("Error fetching player list", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to fetch player list");
        }
    }

    private java.util.Map<String, String> createResponse(String status, String message) {
        java.util.Map<String, String> response = new HashMap<>();
        response.put("status", status);
        response.put("message", message);
        return response;
    }
}

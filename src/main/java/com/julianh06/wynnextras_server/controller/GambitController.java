package com.julianh06.wynnextras_server.controller;

import com.julianh06.wynnextras_server.dto.GambitSubmissionDto;
import com.julianh06.wynnextras_server.service.GambitService;
import com.julianh06.wynnextras_server.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("/gambit")
public class GambitController {
    private static final Logger logger = LoggerFactory.getLogger(GambitController.class);

    @Autowired
    private GambitService gambitService;

    @Autowired
    private AuthService mojangAuth;

    /**
     * Submit today's gambits
     * POST /gambit
     *
     * Supports dual authentication:
     * 1. Mojang Sessionserver (new mod): Headers: Username, Server-ID
     * 2. Wynncraft API Key (future/compatibility): Headers: Wynncraft-Api-Key, Player-UUID
     *
     * Body: { "gambits": [{"name": "...", "description": "..."}] }
     */
    @PostMapping
    public ResponseEntity<?> submitGambits(
            @RequestBody GambitSubmissionDto submission,
            @RequestHeader(value = "Authorization", required = false) String token) {

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createResponse("error", "Missing session token", null));
        }

        // Validate session
        AuthService.SessionData session = AuthService.validateSession(token);

        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createResponse("error", "Session expired or invalid", null));
        }

        String verifiedUsername = session.username;

        // Submit gambits
        try {
            GambitSubmissionDto approved = gambitService.submitGambits(
                submission.getGambits(),
                verifiedUsername
            );

            if (approved != null) {
                logger.info("Gambits were approved (submitted by {})", verifiedUsername);
                return ResponseEntity.ok().body(createResponse(
                    "approved",
                    "Gambits approved for today",
                    approved
                ));
            } else {
                logger.info("Gambits submitted by {} but not yet approved", verifiedUsername);
                return ResponseEntity.ok().body(createResponse(
                    "submitted",
                    "Gambits submitted. Waiting for more confirmations.",
                    null
                ));
            }
        } catch (Exception e) {
            logger.error("Error submitting gambits", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing submission");
        }
    }

    /**
     * Get today's approved gambits
     * GET /gambit
     */
    @GetMapping
    public ResponseEntity<?> getGambits() {
        GambitSubmissionDto gambits = gambitService.getApprovedGambits();

        if (gambits != null) {
            return ResponseEntity.ok(gambits);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No approved gambits for today");
        }
    }

    private java.util.Map<String, Object> createResponse(String status, String message, Object data) {
        java.util.Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("message", message);
        if (data != null) {
            response.put("gambits", data);
        }
        return response;
    }
}

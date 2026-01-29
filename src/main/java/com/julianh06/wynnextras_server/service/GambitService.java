package com.julianh06.wynnextras_server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.julianh06.wynnextras_server.dto.GambitSubmissionDto;
import com.julianh06.wynnextras_server.entity.GambitApproved;
import com.julianh06.wynnextras_server.entity.GambitSubmission;
import com.julianh06.wynnextras_server.repository.GambitApprovedRepository;
import com.julianh06.wynnextras_server.repository.GambitSubmissionRepository;
import com.julianh06.wynnextras_server.repository.VerifiedUserRepository;
import com.julianh06.wynnextras_server.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GambitService {

    private static final Logger logger = LoggerFactory.getLogger(GambitService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private GambitSubmissionRepository submissionRepo;

    @Autowired
    private GambitApprovedRepository approvedRepo;

    @Autowired
    private VerifiedUserRepository verifiedUserRepo;

    /**
     * Submit gambits for today
     * Returns the approved gambits if submission triggers approval, null otherwise
     */
    @Transactional
    public GambitSubmissionDto submitGambits(
            List<GambitSubmissionDto.GambitDto> gambits,
            String username
    ) {
        String dayId = TimeUtils.getDayIdentifier();

        // Check if already approved and locked
        Optional<GambitApproved> existingApproved = approvedRepo.findByDayIdentifier(dayId);
        if (existingApproved.isPresent() && existingApproved.get().isLocked()) {
            logger.info("Gambits for day {} are locked, ignoring submission from {}", dayId, username);
            return deserializeGambits(existingApproved.get().getGambitsJson());
        }

        // Sort gambits for deterministic comparison
        List<GambitSubmissionDto.GambitDto> sortedGambits = gambits.stream()
                .sorted(Comparator.comparing(GambitSubmissionDto.GambitDto::getName))
                .collect(Collectors.toList());

        String gambitsJson = serializeGambits(sortedGambits);

        // Same-user check (1 submission per user per day)
        List<GambitSubmission> userExistingSubmissions =
                submissionRepo.findByDayIdentifierAndSubmittedByOrderBySubmittedAtDesc(dayId, username);

        if (!userExistingSubmissions.isEmpty()) {
            GambitSubmission mostRecent = userExistingSubmissions.get(0);

            // Cleanup legacy duplicates
            if (userExistingSubmissions.size() > 1) {
                for (int i = 1; i < userExistingSubmissions.size(); i++) {
                    submissionRepo.delete(userExistingSubmissions.get(i));
                }
                logger.info(
                        "Cleaned up {} duplicate gambit submissions for day {} from {}",
                        userExistingSubmissions.size() - 1,
                        dayId,
                        username
                );
            }

            mostRecent.setGambitsJson(gambitsJson);
            mostRecent.setSubmittedAt(Instant.now());
            submissionRepo.save(mostRecent);

            logger.info("Updated gambit submission for day {} from {}", dayId, username);
        } else {
            GambitSubmission submission = new GambitSubmission(gambitsJson, username, dayId);
            submissionRepo.save(submission);

            logger.info("Saved new gambit submission for day {} from {}", dayId, username);
        }

        return checkAndApprove(dayId, gambitsJson, username);
    }

    /**
     * Get the approved gambits for today
     */
    public GambitSubmissionDto getApprovedGambits() {
        String dayId = TimeUtils.getDayIdentifier();
        return approvedRepo.findByDayIdentifier(dayId)
                .map(a -> deserializeGambits(a.getGambitsJson()))
                .orElse(null);
    }

    /**
     * Verified user check
     */
    public boolean isVerifiedUser(String username) {
        return verifiedUserRepo.existsByUsername(username.toLowerCase());
    }

    /**
     * Approval logic (mirrors LootPoolService)
     */
    @Transactional
    protected GambitSubmissionDto checkAndApprove(
            String dayId,
            String gambitsJson,
            String submittingUsername
    ) {

        // Verified user → instant approval
        if (isVerifiedUser(submittingUsername)) {
            logger.info(
                    "Verified user {} submitted gambits for day {}, auto-approving",
                    submittingUsername,
                    dayId
            );

            GambitApproved approved = approvedRepo
                    .findByDayIdentifier(dayId)
                    .orElse(new GambitApproved(gambitsJson, dayId, false));

            approved.setGambitsJson(gambitsJson);
            approvedRepo.save(approved);

            return deserializeGambits(gambitsJson);
        }

        // Get matching submissions
        List<GambitSubmission> allSubmissions =
                submissionRepo.findByDayIdentifier(dayId);

        List<GambitSubmission> matchingSubmissions = allSubmissions.stream()
                .filter(s -> s.getGambitsJson().equals(gambitsJson))
                .collect(Collectors.toList());

        long uniqueUserCount = matchingSubmissions.stream()
                .map(GambitSubmission::getSubmittedBy)
                .distinct()
                .count();

        logger.info(
                "Found {} unique users with matching gambits for day {}",
                uniqueUserCount,
                dayId
        );

        // Lock at 10+
        if (uniqueUserCount >= 10) {
            logger.info("Locking gambits for day {} with {} unique users", dayId, uniqueUserCount);

            GambitApproved approved = approvedRepo
                    .findByDayIdentifier(dayId)
                    .orElse(new GambitApproved(gambitsJson, dayId, true));

            approved.setGambitsJson(gambitsJson);
            approved.setLocked(true);
            approvedRepo.save(approved);

            return deserializeGambits(gambitsJson);
        }

        // Approve at 3+
        if (uniqueUserCount >= 3) {
            logger.info("Approving gambits for day {} with {} unique users", dayId, uniqueUserCount);

            GambitApproved approved = approvedRepo
                    .findByDayIdentifier(dayId)
                    .orElse(new GambitApproved(gambitsJson, dayId, false));

            approved.setGambitsJson(gambitsJson);
            approved.setLocked(false);
            approvedRepo.save(approved);

            return deserializeGambits(gambitsJson);
        }

        logger.info(
                "Not enough unique users yet ({}/3) for day {}",
                uniqueUserCount,
                dayId
        );
        return null;
    }

    /* ===================== JSON ===================== */

    private String serializeGambits(List<GambitSubmissionDto.GambitDto> gambits) {
        try {
            return objectMapper.writeValueAsString(gambits);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize gambits", e);
        }
    }

    private GambitSubmissionDto deserializeGambits(String json) {
        try {
            List<GambitSubmissionDto.GambitDto> gambits = objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, GambitSubmissionDto.GambitDto.class)
            );
            return new GambitSubmissionDto(gambits);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize gambits", e);
        }
    }
}
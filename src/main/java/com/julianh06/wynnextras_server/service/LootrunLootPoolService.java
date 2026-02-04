package com.julianh06.wynnextras_server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.julianh06.wynnextras_server.dto.LootrunLootPoolSubmissionDto;
import com.julianh06.wynnextras_server.entity.LootrunLootPoolApproved;
import com.julianh06.wynnextras_server.entity.LootrunLootPoolSubmission;
import com.julianh06.wynnextras_server.repository.LootrunLootPoolApprovedRepository;
import com.julianh06.wynnextras_server.repository.LootrunLootPoolSubmissionRepository;
import com.julianh06.wynnextras_server.repository.VerifiedUserRepository;
import com.julianh06.wynnextras_server.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LootrunLootPoolService {
    private static final Logger logger = LoggerFactory.getLogger(LootrunLootPoolService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    @Autowired
    private LootrunLootPoolSubmissionRepository submissionRepo;

    @Autowired
    private LootrunLootPoolApprovedRepository approvedRepo;

    @Autowired
    private VerifiedUserRepository verifiedUserRepo;

    /**
     * Submit a loot pool for a lootrun
     * Returns the approved pool if submission triggers approval, null otherwise
     */
    @Transactional
    public LootrunLootPoolSubmissionDto submitLootPool(String lootrunType, List<LootrunLootPoolSubmissionDto.ItemDto> items, String username) {
        String weekId = TimeUtils.getLootrunWeekIdentifier();

        // Check if already approved and locked for this week
        Optional<LootrunLootPoolApproved> existing = approvedRepo.findFirstByLootrunTypeAndWeekIdentifierOrderByApprovedAtDesc(lootrunType, weekId);
        if (existing.isPresent() && existing.get().isLocked()) {
            logger.info("Loot pool for {} week {} is locked, ignoring submission from {}", lootrunType, weekId, username);
            return deserializeItems(existing.get().getItemsJson());
        }

        // Sort items by name for comparison
        Comparator<String> stringComparator = Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER);
        List<LootrunLootPoolSubmissionDto.ItemDto> sortedItems = items.stream()
            .sorted(Comparator.comparing(LootrunLootPoolSubmissionDto.ItemDto::getName, stringComparator)
                .thenComparing(LootrunLootPoolSubmissionDto.ItemDto::getRarity, stringComparator)
                .thenComparing(LootrunLootPoolSubmissionDto.ItemDto::getType, stringComparator)
                .thenComparing(LootrunLootPoolSubmissionDto.ItemDto::getShinyStat, stringComparator)
                .thenComparing(LootrunLootPoolSubmissionDto.ItemDto::getTooltip, stringComparator))
            .collect(Collectors.toList());

        String itemsJson = serializeItems(sortedItems);

        // Check if user already submitted for this lootrun/week (sorted by most recent first)
        List<LootrunLootPoolSubmission> userExistingSubmissions =
            submissionRepo.findByLootrunTypeAndWeekIdentifierAndSubmittedByOrderBySubmittedAtDesc(lootrunType, weekId, username);

        if (!userExistingSubmissions.isEmpty()) {
            // Keep most recent, delete old duplicates (legacy cleanup)
            LootrunLootPoolSubmission mostRecent = userExistingSubmissions.get(0);

            if (userExistingSubmissions.size() > 1) {
                // Delete legacy duplicates
                for (int i = 1; i < userExistingSubmissions.size(); i++) {
                    submissionRepo.delete(userExistingSubmissions.get(i));
                }
                logger.info("Cleaned up {} duplicate submissions for {} week {} from {}",
                    userExistingSubmissions.size() - 1, lootrunType, weekId, username);
            }

            // Update the most recent submission
            mostRecent.setItemsJson(itemsJson);
            mostRecent.setSubmittedAt(java.time.Instant.now());
            submissionRepo.save(mostRecent);
            logger.info("Updated loot pool submission for {} week {} from {}", lootrunType, weekId, username);
        } else {
            // Save new submission
            LootrunLootPoolSubmission submission = new LootrunLootPoolSubmission(lootrunType, username, itemsJson, weekId);
            submissionRepo.save(submission);
            logger.info("Saved new loot pool submission for {} week {} from {}", lootrunType, weekId, username);
        }

        // Check if should approve
        return checkAndApprove(lootrunType, weekId, itemsJson, username);
    }

    /**
     * Get the approved loot pool for a lootrun in the current week
     */
    public LootrunLootPoolSubmissionDto getApprovedLootPool(String lootrunType) {
        String weekId = TimeUtils.getLootrunWeekIdentifier();
        Optional<LootrunLootPoolApproved> approved = approvedRepo.findFirstByLootrunTypeAndWeekIdentifierOrderByApprovedAtDesc(lootrunType, weekId);

        if (approved.isPresent()) {
            return deserializeItems(approved.get().getItemsJson());
        }

        return null;
    }

    /**
     * Check if username is in verified user list
     * Case-insensitive check (usernames stored lowercase in DB)
     */
    public boolean isVerifiedUser(String username) {
        return verifiedUserRepo.existsByUsername(username.toLowerCase());
    }

    /**
     * Check if this submission should trigger approval
     * Returns the approved pool if approved, null otherwise
     */
    @Transactional
    protected LootrunLootPoolSubmissionDto checkAndApprove(String lootrunType, String weekId, String itemsJson, String submittingUsername) {
        // Check if verified user - instant approval
        if (isVerifiedUser(submittingUsername)) {
            logger.info("Verified user {} submitted loot pool for {} week {}, auto-approving", submittingUsername, lootrunType, weekId);
            LootrunLootPoolApproved approved = new LootrunLootPoolApproved(lootrunType, itemsJson, weekId, 1, false);
            approvedRepo.save(approved);
            return deserializeItems(itemsJson);
        }

        // Get all submissions for this lootrun/week with matching JSON
        List<LootrunLootPoolSubmission> allSubmissions = submissionRepo.findByLootrunTypeAndWeekIdentifier(lootrunType, weekId);
        List<LootrunLootPoolSubmission> matchingSubmissions = allSubmissions.stream()
            .filter(s -> s.getItemsJson().equals(itemsJson))
            .collect(Collectors.toList());

        // Count UNIQUE users who submitted this exact loot pool
        long uniqueUserCount = matchingSubmissions.stream()
            .map(LootrunLootPoolSubmission::getSubmittedBy)
            .distinct()
            .count();

        logger.info("Found {} unique users with matching submissions for {} week {}", uniqueUserCount, lootrunType, weekId);

        // Check for lock (10+ unique users)
        if (uniqueUserCount >= 10) {
            logger.info("Locking loot pool for {} week {} with {} unique users", lootrunType, weekId, uniqueUserCount);

            Optional<LootrunLootPoolApproved> existing = approvedRepo.findFirstByLootrunTypeAndWeekIdentifierOrderByApprovedAtDesc(lootrunType, weekId);
            if (existing.isPresent()) {
                LootrunLootPoolApproved approved = existing.get();
                approved.setLocked(true);
                approved.setSubmissionCount((int) uniqueUserCount);
                approvedRepo.save(approved);
            } else {
                LootrunLootPoolApproved approved = new LootrunLootPoolApproved(lootrunType, itemsJson, weekId, (int) uniqueUserCount, true);
                approvedRepo.save(approved);
            }
            return deserializeItems(itemsJson);
        }

        // Check for approval (3+ unique users)
        if (uniqueUserCount >= 3) {
            logger.info("Approving loot pool for {} week {} with {} unique users", lootrunType, weekId, uniqueUserCount);

            Optional<LootrunLootPoolApproved> existing = approvedRepo.findFirstByLootrunTypeAndWeekIdentifierOrderByApprovedAtDesc(lootrunType, weekId);
            if (existing.isPresent()) {
                LootrunLootPoolApproved approved = existing.get();
                approved.setItemsJson(itemsJson);
                approved.setSubmissionCount((int) uniqueUserCount);
                approvedRepo.save(approved);
            } else {
                LootrunLootPoolApproved approved = new LootrunLootPoolApproved(lootrunType, itemsJson, weekId, (int) uniqueUserCount, false);
                approvedRepo.save(approved);
            }
            return deserializeItems(itemsJson);
        }

        logger.info("Not enough unique users yet ({}/3) for {} week {}", uniqueUserCount, lootrunType, weekId);
        return null;
    }

    private String serializeItems(List<LootrunLootPoolSubmissionDto.ItemDto> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize items", e);
        }
    }

    private LootrunLootPoolSubmissionDto deserializeItems(String json) {
        try {
            List<LootrunLootPoolSubmissionDto.ItemDto> items = objectMapper.readValue(
                json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, LootrunLootPoolSubmissionDto.ItemDto.class)
            );
            return new LootrunLootPoolSubmissionDto(items);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize items", e);
        }
    }
}

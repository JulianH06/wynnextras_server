package com.julianh06.wynnextras_server.controller;

import com.julianh06.wynnextras_server.dto.TradeMarketListingDto;
import com.julianh06.wynnextras_server.entity.TradeMarketListing;
import com.julianh06.wynnextras_server.repository.TradeMarketListingRepository;
import com.julianh06.wynnextras_server.service.AuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/trademarket")
public class TradeMarketController {
    private static final Logger logger = LoggerFactory.getLogger(TradeMarketController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TradeMarketListingRepository repository;

    @PostMapping("/listings")
    public ResponseEntity<?> submitListings(
            @RequestBody TradeMarketListingDto.BatchSubmission submission,
            @RequestHeader(value = "Authorization", required = false) String token) {

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing session token");
        }

        AuthService.SessionData session = AuthService.validateSession(token);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Session expired or invalid");
        }

        String verifiedUsername = session.username;
        String verifiedUuid = session.uuid;

        if (submission.getListings() == null || submission.getListings().isEmpty()) {
            return ResponseEntity.badRequest().body("No listings provided");
        }

        List<TradeMarketListing> entities = new ArrayList<>();
        for (TradeMarketListingDto.ListingData listing : submission.getListings()) {
            if (listing.getName() == null || listing.getName().isBlank()) continue;
            if (listing.getListingPrice() <= 0) continue;

            String statsJson = null;
            if (listing.getStats() != null && !listing.getStats().isEmpty()) {
                try {
                    statsJson = objectMapper.writeValueAsString(listing.getStats());
                } catch (JsonProcessingException e) {
                    logger.warn("Failed to serialize stats for item {}", listing.getName());
                }
            }

            entities.add(new TradeMarketListing(
                    verifiedUsername,
                    verifiedUuid,
                    listing.getName(),
                    listing.getRarity(),
                    listing.getItemType(),
                    listing.getType(),
                    listing.getListingPrice(),
                    listing.getOverallPercentage(),
                    statsJson,
                    listing.getShinyStat(),
                    listing.getTimestamp()
            ));
        }

        if (entities.isEmpty()) {
            return ResponseEntity.badRequest().body("No valid listings in submission");
        }

        repository.saveAll(entities);
        logger.info("Saved {} trade market listings from {}", entities.size(), verifiedUsername);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("count", entities.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/listings")
    public ResponseEntity<?> getListings(
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) String rarity,
            @RequestParam(defaultValue = "24") int hours) {

        hours = Math.max(1, Math.min(hours, 720));
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);

        List<TradeMarketListing> listings;
        if (itemName != null && rarity != null) {
            listings = repository.findByItemNameIgnoreCaseAndRarityAndSubmittedAtAfter(itemName, rarity, since);
        } else if (itemName != null) {
            listings = repository.findByItemNameIgnoreCaseAndSubmittedAtAfter(itemName, since);
        } else if (rarity != null) {
            listings = repository.findByRarityAndSubmittedAtAfter(rarity, since);
        } else {
            listings = repository.findBySubmittedAtAfter(since);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (TradeMarketListing listing : listings) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", listing.getItemName());
            item.put("rarity", listing.getRarity());
            item.put("itemType", listing.getItemType());
            item.put("type", listing.getType());
            item.put("listingPrice", listing.getListingPrice());
            item.put("overallPercentage", listing.getOverallPct());
            item.put("shinyStat", listing.getShinyStat());
            item.put("submittedBy", listing.getSubmittedBy());
            item.put("submittedAt", listing.getSubmittedAt().toString());

            if (listing.getStatsJson() != null) {
                try {
                    item.put("stats", objectMapper.readValue(listing.getStatsJson(), List.class));
                } catch (JsonProcessingException e) {
                    item.put("stats", Collections.emptyList());
                }
            } else {
                item.put("stats", Collections.emptyList());
            }

            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Instant last24h = Instant.now().minus(24, ChronoUnit.HOURS);
        long total = repository.count();
        long recent = repository.countBySubmittedAtAfter(last24h);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalListings", total);
        stats.put("last24h", recent);
        return ResponseEntity.ok(stats);
    }

    @Scheduled(cron = "0 0 3 * * *") // daily at 3 AM
    @Transactional
    public void cleanupOldListings() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        repository.deleteBySubmittedAtBefore(cutoff);
        logger.info("Cleaned up trade market listings older than 30 days");
    }
}

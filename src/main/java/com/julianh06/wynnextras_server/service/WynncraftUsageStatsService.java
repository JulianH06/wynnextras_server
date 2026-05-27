package com.julianh06.wynnextras_server.service;

import com.julianh06.wynnextras_server.WynncraftService;
import com.julianh06.wynnextras_server.entity.WynncraftPlayerSighting;
import com.julianh06.wynnextras_server.entity.WynncraftUsageSnapshot;
import com.julianh06.wynnextras_server.repository.WynncraftPlayerSightingRepository;
import com.julianh06.wynnextras_server.repository.WynncraftUsageSnapshotRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Service
public class WynncraftUsageStatsService {
    private static final Logger logger = LoggerFactory.getLogger(WynncraftUsageStatsService.class);

    private final WynncraftService wynncraftService;
    private final WynncraftPlayerSightingRepository playerSightingRepository;
    private final WynncraftUsageSnapshotRepository usageSnapshotRepository;

    public WynncraftUsageStatsService(
            WynncraftService wynncraftService,
            WynncraftPlayerSightingRepository playerSightingRepository,
            WynncraftUsageSnapshotRepository usageSnapshotRepository) {
        this.wynncraftService = wynncraftService;
        this.playerSightingRepository = playerSightingRepository;
        this.usageSnapshotRepository = usageSnapshotRepository;
    }

    @Scheduled(cron = "0 5 * * * *", zone = "UTC")
    public void captureOnlinePlayerSample() {
        Instant sampledAt = Instant.now();

        try {
            int visiblePlayerCount = captureOnlinePlayerSample(sampledAt);
            logger.info("Captured Wynncraft online player sample: {} visible UUIDs", visiblePlayerCount);
        } catch (Exception e) {
            logger.warn("Failed to capture Wynncraft online player sample: {}", e.getMessage());
        }
    }

    public int captureOnlinePlayerSample(Instant sampledAt) {
        Set<String> onlinePlayerUuids = wynncraftService.fetchOnlinePlayerUuids();
        storeOnlinePlayerSample(sampledAt, onlinePlayerUuids);
        return onlinePlayerUuids.size();
    }

    public void storeOnlinePlayerSample(Instant sampledAt, Set<String> onlinePlayerUuids) {
        if (onlinePlayerUuids == null || onlinePlayerUuids.isEmpty()) {
            return;
        }

        playerSightingRepository.saveAll(onlinePlayerUuids.stream()
                .filter(uuid -> uuid != null && !uuid.isBlank())
                .map(WynncraftUsageStatsService::normalizeUuid)
                .filter(uuid -> uuid.matches("[0-9a-f]{32}"))
                .distinct()
                .map(uuid -> new WynncraftPlayerSighting(uuid, sampledAt))
                .toList());
    }

    @Transactional
    public WynncraftUsageSnapshot captureDailyUsageSnapshot(LocalDate snapshotDate, Instant snapshotInstant) {
        WynncraftUsageSnapshot snapshot = usageSnapshotRepository.findBySnapshotDate(snapshotDate)
                .orElseGet(() -> new WynncraftUsageSnapshot(snapshotDate, snapshotInstant));
        snapshot.setCapturedAt(snapshotInstant);

        try {
            Instant dayStart = snapshotDate.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant nextDayStart = snapshotDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant dayEnd = snapshotInstant.isBefore(nextDayStart) ? snapshotInstant : nextDayStart;

            long uniquePlayers = playerSightingRepository.countUniquePlayersSeenInRange(dayStart, dayEnd);
            long wynnExtrasUsers = playerSightingRepository.countDailyActiveWynnExtrasUsersSeenBetween(dayStart, dayEnd, snapshotDate);
            long sampleCount = playerSightingRepository.countSamplesBetween(dayStart, dayEnd);

            snapshot.setUniquePlayers(uniquePlayers);
            snapshot.setWynnExtrasUsers(wynnExtrasUsers);
            snapshot.setSampleCount(sampleCount);
            snapshot.setUsagePercent(uniquePlayers == 0 ? 0.0 : (double) wynnExtrasUsers * 100.0 / uniquePlayers);
            snapshot.setErrorMessage(null);

            usageSnapshotRepository.save(snapshot);
            playerSightingRepository.deleteBySampledAtBefore(snapshotInstant.minus(14, ChronoUnit.DAYS));
        } catch (Exception e) {
            snapshot.setErrorMessage(e.getMessage());
            usageSnapshotRepository.save(snapshot);
            logger.warn("Failed to capture Wynncraft usage snapshot for {}: {}", snapshotDate, e.getMessage());
        }

        return snapshot;
    }

    private static String normalizeUuid(String uuid) {
        return uuid.replace("-", "").toLowerCase();
    }
}

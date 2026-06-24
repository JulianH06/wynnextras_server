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
import java.util.List;
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
            CapturedOnlinePlayerSample sample = captureOnlinePlayerSample(sampledAt);
            logger.info("Captured Wynncraft online player sample: {} visible UUIDs, {} total online players",
                    sample.visiblePlayers(), sample.totalOnlinePlayers());
        } catch (Exception e) {
            logger.warn("Failed to capture Wynncraft online player sample: {}", e.getMessage());
        }
    }

    public CapturedOnlinePlayerSample captureOnlinePlayerSample(Instant sampledAt) {
        WynncraftService.OnlinePlayerSample sample = wynncraftService.fetchOnlinePlayerSample();
        storeOnlinePlayerSample(sampledAt, sample.playerUuids());
        return new CapturedOnlinePlayerSample(sample.playerUuids().size(), sample.totalOnlinePlayers());
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

    public List<UsageSampleBreakdown> buildSampleBreakdown(LocalDate snapshotDate) {
        Instant dayStart = snapshotDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant nextDayStart = snapshotDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        return buildSampleBreakdownBetween(dayStart, nextDayStart);
    }

    public List<UsageSampleBreakdown> buildSampleBreakdownUntil(LocalDate snapshotDate, Instant snapshotInstant) {
        Instant dayStart = snapshotDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant nextDayStart = snapshotDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant dayEnd = snapshotInstant.isBefore(nextDayStart) ? snapshotInstant.plusNanos(1) : nextDayStart;

        return buildSampleBreakdownBetween(dayStart, dayEnd);
    }

    public UsageSampleStats buildSampleStats(List<UsageSampleBreakdown> samples) {
        if (samples == null || samples.isEmpty()) {
            return UsageSampleStats.empty();
        }

        double totalUsagePercent = 0.0;
        double lowestUsagePercent = Double.MAX_VALUE;
        double highestUsagePercent = Double.NEGATIVE_INFINITY;
        long totalVisiblePlayers = 0;
        long totalWynnExtrasUsers = 0;

        for (UsageSampleBreakdown sample : samples) {
            double usagePercent = sample.usagePercent();
            totalUsagePercent += usagePercent;
            lowestUsagePercent = Math.min(lowestUsagePercent, usagePercent);
            highestUsagePercent = Math.max(highestUsagePercent, usagePercent);
            totalVisiblePlayers += sample.visiblePlayers();
            totalWynnExtrasUsers += sample.wynnExtrasUsers();
        }

        return new UsageSampleStats(
                samples.size(),
                totalUsagePercent / samples.size(),
                lowestUsagePercent,
                highestUsagePercent,
                Math.round((double) totalVisiblePlayers / samples.size()),
                Math.round((double) totalWynnExtrasUsers / samples.size()));
    }

    private List<UsageSampleBreakdown> buildSampleBreakdownBetween(Instant dayStart, Instant dayEnd) {
        return playerSightingRepository
                .findUsageSampleBreakdownBetween(dayStart, dayEnd)
                .stream()
                .map(row -> {
                    long visiblePlayers = row.getVisiblePlayers();
                    long wynnExtrasUsers = row.getWynnExtrasUsers();
                    double usagePercent = visiblePlayers == 0
                            ? 0.0
                            : (double) wynnExtrasUsers * 100.0 / visiblePlayers;
                    return new UsageSampleBreakdown(
                            row.getSampledAt(),
                            visiblePlayers,
                            wynnExtrasUsers,
                            usagePercent);
                })
                .toList();
    }

    @Transactional
    public WynncraftUsageSnapshot captureDailyUsageSnapshot(LocalDate snapshotDate, Instant snapshotInstant) {
        return captureDailyUsageSnapshot(snapshotDate, snapshotInstant, null);
    }

    @Transactional
    public WynncraftUsageSnapshot captureDailyUsageSnapshot(
            LocalDate snapshotDate,
            Instant snapshotInstant,
            Long totalOnlinePlayers) {
        WynncraftUsageSnapshot snapshot = usageSnapshotRepository.findBySnapshotDate(snapshotDate)
                .orElseGet(() -> new WynncraftUsageSnapshot(snapshotDate, snapshotInstant));
        snapshot.setCapturedAt(snapshotInstant);
        if (totalOnlinePlayers != null) {
            snapshot.setTotalOnlinePlayers(totalOnlinePlayers);
        }

        try {
            Instant dayStart = snapshotDate.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant nextDayStart = snapshotDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant dayEnd = snapshotInstant.isBefore(nextDayStart) ? snapshotInstant.plusNanos(1) : nextDayStart;

            long uniquePlayers = playerSightingRepository.countUniquePlayersSeenInRange(dayStart, dayEnd);
            long wynnExtrasUsers = playerSightingRepository.countDailyActiveWynnExtrasUsersSeenBetween(dayStart, dayEnd, snapshotDate);
            long sampleCount = playerSightingRepository.countSamplesBetween(dayStart, dayEnd);
            UsageSampleStats sampleStats = buildSampleStats(buildSampleBreakdownUntil(snapshotDate, snapshotInstant));

            snapshot.setUniquePlayers(uniquePlayers);
            snapshot.setWynnExtrasUsers(wynnExtrasUsers);
            snapshot.setSampleCount(sampleCount);
            snapshot.setUsagePercent(sampleStats.sampleCount() == 0 ? 0.0 : sampleStats.averageUsagePercent());
            snapshot.setErrorMessage(null);

            usageSnapshotRepository.save(snapshot);
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

    public record CapturedOnlinePlayerSample(int visiblePlayers, int totalOnlinePlayers) {}

    public record UsageSampleBreakdown(
            Instant sampledAt,
            long visiblePlayers,
            long wynnExtrasUsers,
            double usagePercent) {}

    public record UsageSampleStats(
            long sampleCount,
            double averageUsagePercent,
            double lowestUsagePercent,
            double highestUsagePercent,
            long averageVisiblePlayers,
            long averageWynnExtrasUsers) {
        public static UsageSampleStats empty() {
            return new UsageSampleStats(0, 0.0, 0.0, 0.0, 0, 0);
        }
    }
}

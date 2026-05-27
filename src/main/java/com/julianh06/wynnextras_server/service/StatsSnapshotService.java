package com.julianh06.wynnextras_server.service;

import com.julianh06.wynnextras_server.entity.ActiveUserSnapshot;
import com.julianh06.wynnextras_server.entity.GuildUserSnapshot;
import com.julianh06.wynnextras_server.entity.VersionUsageSnapshot;
import com.julianh06.wynnextras_server.entity.WynnExtrasUser;
import com.julianh06.wynnextras_server.repository.ActiveUserSnapshotRepository;
import com.julianh06.wynnextras_server.repository.GuildUserSnapshotRepository;
import com.julianh06.wynnextras_server.repository.VersionUsageSnapshotRepository;
import com.julianh06.wynnextras_server.repository.WynnExtrasUserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsSnapshotService {
    private static final Logger logger = LoggerFactory.getLogger(StatsSnapshotService.class);

    private static final List<String> TRACKED_GUILDS = List.of(
            "SEQ", "Aeq", "AVO", "ANO", "ESI", "Nia", "PUN", "HOC", "DUDE",
            "TAq", "BFS", "ICo", "PROF", "Zamn", "HSP", "TBGM", "DEU", "Tsd"
    );

    @Autowired private WynnExtrasUserRepository wynnExtrasUserRepository;
    @Autowired private ActiveUserSnapshotRepository activeUserSnapshotRepository;
    @Autowired private VersionUsageSnapshotRepository versionUsageSnapshotRepository;
    @Autowired private GuildUserSnapshotRepository guildUserSnapshotRepository;
    @Autowired private GuildStatsService guildStatsService;
    @Autowired private WynncraftUsageStatsService wynncraftUsageStatsService;

    @Scheduled(cron = "0 0 1 * * *", zone = "UTC")
    public void captureDailySnapshots() {
        Instant snapshotInstant = Instant.now();
        LocalDate snapshotDate = LocalDate.ofInstant(snapshotInstant, ZoneOffset.UTC);
        captureSnapshots(snapshotDate, snapshotInstant);
    }

    @Transactional
    public void captureSnapshots(LocalDate snapshotDate, Instant snapshotInstant) {
        logger.info("Capturing daily stats snapshots for {}", snapshotDate);
        captureActiveUserSnapshot(snapshotDate, snapshotInstant);
        captureVersionUsageSnapshots(snapshotDate, snapshotInstant);
        Long totalOnlinePlayers = null;
        try {
            WynncraftUsageStatsService.CapturedOnlinePlayerSample sample =
                    wynncraftUsageStatsService.captureOnlinePlayerSample(snapshotInstant);
            totalOnlinePlayers = (long) sample.totalOnlinePlayers();
        } catch (Exception e) {
            logger.warn("Failed to capture current Wynncraft online player total for daily snapshot: {}", e.getMessage());
        }
        wynncraftUsageStatsService.captureDailyUsageSnapshot(snapshotDate, snapshotInstant, totalOnlinePlayers);
        captureGuildSnapshots(snapshotDate, snapshotInstant);
    }

    private void captureActiveUserSnapshot(LocalDate snapshotDate, Instant snapshotInstant) {
        ActiveUserSnapshot snapshot = activeUserSnapshotRepository.findBySnapshotDate(snapshotDate)
                .orElseGet(() -> new ActiveUserSnapshot(snapshotDate, snapshotInstant));

        snapshot.setCapturedAt(snapshotInstant);
        snapshot.setActive1d(wynnExtrasUserRepository.countActiveUsersSince(snapshotInstant.minus(1, ChronoUnit.DAYS)));
        snapshot.setActive3d(wynnExtrasUserRepository.countActiveUsersSince(snapshotInstant.minus(3, ChronoUnit.DAYS)));
        snapshot.setActive5d(wynnExtrasUserRepository.countActiveUsersSince(snapshotInstant.minus(5, ChronoUnit.DAYS)));
        snapshot.setActive7d(wynnExtrasUserRepository.countActiveUsersSince(snapshotInstant.minus(7, ChronoUnit.DAYS)));
        snapshot.setActive10d(wynnExtrasUserRepository.countActiveUsersSince(snapshotInstant.minus(10, ChronoUnit.DAYS)));
        snapshot.setActive14d(wynnExtrasUserRepository.countActiveUsersSince(snapshotInstant.minus(14, ChronoUnit.DAYS)));

        activeUserSnapshotRepository.save(snapshot);
    }

    private void captureVersionUsageSnapshots(LocalDate snapshotDate, Instant snapshotInstant) {
        Instant cutoff1 = snapshotInstant.minus(1, ChronoUnit.DAYS);
        Instant cutoff3 = snapshotInstant.minus(3, ChronoUnit.DAYS);
        Instant cutoff7 = snapshotInstant.minus(7, ChronoUnit.DAYS);
        Instant cutoff14 = snapshotInstant.minus(14, ChronoUnit.DAYS);
        Map<String, VersionCounts> countsByVersion = new HashMap<>();

        for (WynnExtrasUser user : wynnExtrasUserRepository.findAll()) {
            String version = user.getModVersion();
            if (version == null || version.isBlank()) {
                continue;
            }
            VersionCounts counts = countsByVersion.computeIfAbsent(version, ignored -> new VersionCounts());
            counts.total++;
            if (user.getLastSeen() != null && user.getLastSeen().isAfter(cutoff1)) {
                counts.active1++;
            }
            if (user.getLastSeen() != null && user.getLastSeen().isAfter(cutoff3)) {
                counts.active3++;
            }
            if (user.getLastSeen() != null && user.getLastSeen().isAfter(cutoff7)) {
                counts.active7++;
            }
            if (user.getLastSeen() != null && user.getLastSeen().isAfter(cutoff14)) {
                counts.active14++;
            }
        }

        for (Map.Entry<String, VersionCounts> entry : countsByVersion.entrySet()) {
            VersionUsageSnapshot snapshot = versionUsageSnapshotRepository
                    .findBySnapshotDateAndModVersion(snapshotDate, entry.getKey())
                    .orElseGet(() -> new VersionUsageSnapshot(snapshotDate, snapshotInstant, entry.getKey()));
            VersionCounts counts = entry.getValue();
            snapshot.setCapturedAt(snapshotInstant);
            snapshot.setUserCount(counts.total);
            snapshot.setActive1dCount(counts.active1);
            snapshot.setActive3dCount(counts.active3);
            snapshot.setActive7dCount(counts.active7);
            snapshot.setActive14dCount(counts.active14);
            versionUsageSnapshotRepository.save(snapshot);
        }
    }

    private void captureGuildSnapshots(LocalDate snapshotDate, Instant snapshotInstant) {
        for (String tag : TRACKED_GUILDS) {
            GuildUserSnapshot snapshot = guildUserSnapshotRepository.findBySnapshotDateAndGuildTag(snapshotDate, tag)
                    .orElseGet(() -> new GuildUserSnapshot(snapshotDate, snapshotInstant, tag));
            snapshot.setCapturedAt(snapshotInstant);

            try {
                GuildStatsService.GuildSnapshotStats stats = guildStatsService.buildSnapshotStats(tag, snapshotInstant);
                snapshot.setGuildName(stats.guildName);
                snapshot.setMemberCount(stats.memberCount);
                snapshot.setWynnExtrasUsersTotal(stats.wynnExtrasUsersTotal);
                snapshot.setActive1d(stats.active1d);
                snapshot.setActive3d(stats.active3d);
                snapshot.setActive5d(stats.active5d);
                snapshot.setActive7d(stats.active7d);
                snapshot.setActive10d(stats.active10d);
                snapshot.setActive14d(stats.active14d);
                snapshot.setErrorMessage(null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                snapshot.setErrorMessage("Request interrupted");
                guildUserSnapshotRepository.save(snapshot);
                logger.warn("Interrupted while capturing guild snapshot for {}", tag);
                return;
            } catch (Exception e) {
                snapshot.setErrorMessage(e.getMessage());
                logger.warn("Failed to capture guild snapshot for {}: {}", tag, e.getMessage());
            }

            guildUserSnapshotRepository.save(snapshot);
        }
    }

    private static class VersionCounts {
        long total;
        long active1;
        long active3;
        long active7;
        long active14;
    }
}

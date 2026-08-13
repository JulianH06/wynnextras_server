package com.julianh06.wynnextras_server.service;

import com.julianh06.wynnextras_server.entity.AnonymousDailyActivity;
import com.julianh06.wynnextras_server.entity.AnonymousUserActivity;
import com.julianh06.wynnextras_server.repository.AnonymousDailyActivityRepository;
import com.julianh06.wynnextras_server.repository.AnonymousUserActivityRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
public class AnonymousTelemetryService {
    public static final long PERIOD_SECONDS = Duration.ofDays(30).toSeconds();

    private final AnonymousUserActivityRepository activityRepository;
    private final AnonymousDailyActivityRepository dailyActivityRepository;
    private final Clock clock;

    public AnonymousTelemetryService(AnonymousUserActivityRepository activityRepository,
                                     AnonymousDailyActivityRepository dailyActivityRepository) {
        this(activityRepository, dailyActivityRepository, Clock.systemUTC());
    }

    AnonymousTelemetryService(AnonymousUserActivityRepository activityRepository,
                              AnonymousDailyActivityRepository dailyActivityRepository,
                              Clock clock) {
        this.activityRepository = activityRepository;
        this.dailyActivityRepository = dailyActivityRepository;
        this.clock = clock;
    }

    public static long periodFor(Instant instant) {
        return Math.floorDiv(instant.getEpochSecond(), PERIOD_SECONDS);
    }

    @Transactional
    public synchronized void record(String anonymousId, long period, String modVersion) {
        Instant heartbeatAt = clock.instant();
        if (period != periodFor(heartbeatAt)) {
            throw new IllegalArgumentException("period must be the current 30-day period");
        }

        String normalizedId = normalizeAnonymousId(anonymousId);
        String normalizedVersion = validateModVersion(modVersion);

        AnonymousUserActivity activity = activityRepository.findByAnonymousIdAndPeriod(normalizedId, period)
                .orElseGet(() -> new AnonymousUserActivity(normalizedId, period, normalizedVersion, heartbeatAt));
        if (activity.getId() != null) {
            activity.recordHeartbeat(normalizedVersion, heartbeatAt);
        }
        activityRepository.save(activity);

        LocalDate activityDate = LocalDate.ofInstant(heartbeatAt, ZoneOffset.UTC);
        AnonymousDailyActivity daily = dailyActivityRepository
                .findByActivityDateAndPeriodAndAnonymousId(activityDate, period, normalizedId)
                .orElseGet(() -> new AnonymousDailyActivity(
                        activityDate, period, normalizedId, normalizedVersion, heartbeatAt));
        if (daily.getId() != null) {
            daily.recordHeartbeat(normalizedVersion, heartbeatAt);
        }
        dailyActivityRepository.save(daily);
    }

    static String normalizeAnonymousId(String anonymousId) {
        if (anonymousId == null) {
            throw new IllegalArgumentException("anonymousId is required");
        }
        String normalized = anonymousId.trim().toLowerCase(Locale.ROOT).replace("-", "");
        if (!normalized.matches("[0-9a-f]{32}")) {
            throw new IllegalArgumentException("anonymousId must be a UUID");
        }
        return normalized;
    }

    static String validateModVersion(String modVersion) {
        if (modVersion == null) {
            throw new IllegalArgumentException("modVersion is required");
        }
        String normalized = modVersion.trim();
        if (!normalized.matches("[A-Za-z0-9._+\\-]{1,32}")) {
            throw new IllegalArgumentException("modVersion is invalid");
        }
        return normalized;
    }
}

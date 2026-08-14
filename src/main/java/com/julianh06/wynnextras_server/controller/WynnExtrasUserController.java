package com.julianh06.wynnextras_server.controller;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.julianh06.wynnextras_server.entity.BadgeProfile;
import com.julianh06.wynnextras_server.entity.WynnExtrasUser;
import com.julianh06.wynnextras_server.entity.DailyUserActivity;
import com.julianh06.wynnextras_server.repository.AnonymousUserActivityRepository;
import com.julianh06.wynnextras_server.repository.AnonymousDailyActivityRepository;
import com.julianh06.wynnextras_server.repository.BadgeProfileRepository;
import com.julianh06.wynnextras_server.repository.DailyUserActivityRepository;
import com.julianh06.wynnextras_server.repository.WynnExtrasUserRepository;
import com.julianh06.wynnextras_server.service.AnonymousTelemetryService;
import com.julianh06.wynnextras_server.service.AuthService;
import com.julianh06.wynnextras_server.util.BadgeCatalog;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Controller for WynnExtras user badge (⭐) system.
 *
 * Endpoints:
 * - POST /wynnextras-users/heartbeat - Client heartbeat to register activity
 * - GET /wynnextras-users/active - Get list of active user UUIDs for badge display
 */
@RestController
@RequestMapping("/wynnextras-users")
public class WynnExtrasUserController {
    private static final Logger logger = LoggerFactory.getLogger(WynnExtrasUserController.class);
    private static final Duration ACTIVE_THRESHOLD = Duration.ofDays(7);

    @Autowired
    private WynnExtrasUserRepository userRepository;

    @Autowired
    private DailyUserActivityRepository dailyUserActivityRepository;

    @Autowired
    private BadgeProfileRepository badgeProfileRepository;

    @Autowired
    private AnonymousUserActivityRepository anonymousUserActivityRepository;

    @Autowired
    private AnonymousDailyActivityRepository anonymousDailyActivityRepository;

    @Autowired
    private AnonymousTelemetryService anonymousTelemetryService;

    @Autowired
    private AuthService authService;

    /**
     * Client heartbeat - registers or updates user activity
     * POST /wynnextras-users/heartbeat
     *
     * Headers: Username, Server-ID (Mojang auth)
     * Body: { "modVersion": "0.12.1" }
     *
     * Called on game launch and periodically (every 600 seconds)
     */
    @PostMapping("/heartbeat")
    @Transactional
    public ResponseEntity<?> heartbeat(
            @RequestBody HeartbeatRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status","error","message","Missing session token"));
        }

        AuthService.SessionData session = authService.validateSessionToken(token);

        if (session == null) {
            System.out.println("Session expired");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status","error","message","Session expired or invalid"));
        }

        String verifiedUuid = session.uuid;
        String verifiedUsername = session.username;
        String modVersion = request.getModVersion().trim();
        String badgeIconId = BadgeCatalog.normalizeBadgeIconId(request.getBadgeIconId());
        String badgeColorId = BadgeCatalog.normalizeBadgeColorId(request.getBadgeColorId());
        Instant heartbeatAt = Instant.now();

        try {
            // Find existing user or create new one
            Optional<WynnExtrasUser> existingUser = userRepository.findByUuid(verifiedUuid);

            if (existingUser.isPresent()) {
                // Update existing user
                WynnExtrasUser user = existingUser.get();
                user.setUsername(verifiedUsername); // Update username in case it changed
                user.setModVersion(modVersion);
                user.setBadgeIconId(badgeIconId);
                user.setBadgeColorId(badgeColorId);
                user.setLastSeen(heartbeatAt);
                userRepository.save(user);
                logger.debug("Updated heartbeat for user {} ({})", verifiedUsername, verifiedUuid);
            } else {
                // Create new user
                WynnExtrasUser user = new WynnExtrasUser(verifiedUuid, verifiedUsername, modVersion);
                user.setBadgeIconId(badgeIconId);
                user.setBadgeColorId(badgeColorId);
                user.setLastSeen(heartbeatAt);
                user.setCreatedAt(heartbeatAt);
                userRepository.save(user);
                logger.info("Registered new WynnExtras user: {} ({})", verifiedUsername, verifiedUuid);
            }

            upsertBadgeProfile(
                    verifiedUuid, verifiedUsername, badgeIconId, badgeColorId, null, heartbeatAt, true);
            recordDailyActivity(verifiedUuid, verifiedUsername, modVersion, heartbeatAt);

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Heartbeat recorded"
            ));
        } catch (Exception e) {
            logger.error("Error processing heartbeat for user {}", verifiedUsername, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", "Error processing heartbeat"));
        }
    }

    /**
     * Pseudonymous heartbeat. The identifier is only meaningful in the supplied
     * 30-day period and is never joined to UUID-based data.
     */
    @PostMapping("/anonymous-heartbeat")
    public ResponseEntity<?> anonymousHeartbeat(@RequestBody AnonymousHeartbeatRequest request) {
        if (request.hasUnknownFields()) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "status", "error", "message", "Only anonymousId, period and modVersion are accepted"));
        }
        try {
            anonymousTelemetryService.record(request.getAnonymousId(), request.getPeriod(), request.getModVersion());
            return ResponseEntity.ok(java.util.Map.of(
                    "status", "success", "message", "Anonymous heartbeat recorded"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error processing anonymous heartbeat", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of(
                    "status", "error", "message", "Error processing anonymous heartbeat"));
        }
    }

    /** Updates only badge preferences. It intentionally records no usage activity. */
    @PostMapping("/badge")
    @Transactional
    public ResponseEntity<?> updateBadge(
            @RequestBody BadgeUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("status", "error", "message", "Missing session token"));
        }
        AuthService.SessionData session = authService.validateSessionToken(token);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("status", "error", "message", "Session expired or invalid"));
        }
        if (request.getPublished() == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "status", "error", "message", "published is required"));
        }

        upsertBadgeProfile(
                session.uuid,
                session.username,
                BadgeCatalog.normalizeBadgeIconId(request.getBadgeIconId()),
                BadgeCatalog.normalizeBadgeColorId(request.getBadgeColorId()),
                request.getPublished(),
                Instant.now(),
                false);
        return ResponseEntity.ok(java.util.Map.of("status", "success", "message", "Badge updated"));
    }

    private void upsertBadgeProfile(String uuid, String username, String iconId, String colorId,
                                    Boolean published, Instant badgeLastSeen, boolean updateUsername) {
        BadgeProfile profile = badgeProfileRepository.findById(uuid)
                .orElseGet(() -> new BadgeProfile(uuid, username, badgeLastSeen));
        if (updateUsername) {
            profile.setUsername(username);
        }
        profile.setBadgeIconId(iconId);
        profile.setBadgeColorId(colorId);
        if (published != null) {
            profile.setPublished(published);
        }
        profile.setBadgeLastSeen(badgeLastSeen);
        badgeProfileRepository.save(profile);
    }

    private void recordDailyActivity(String uuid, String username, String modVersion, Instant heartbeatAt) {
        LocalDate activityDate = LocalDate.ofInstant(heartbeatAt, ZoneOffset.UTC);
        DailyUserActivity activity = dailyUserActivityRepository
                .findByActivityDateAndUserUuid(activityDate, uuid)
                .orElseGet(() -> new DailyUserActivity(activityDate, uuid, username, modVersion, heartbeatAt));

        if (activity.getId() != null) {
            activity.recordHeartbeat(username, modVersion, heartbeatAt);
        }

        dailyUserActivityRepository.save(activity);
    }

    /**
     * Get list of active WynnExtras user UUIDs
     * GET /wynnextras-users/active
     *
     * Returns UUIDs of users active within the last 7 days
     * Used by clients to display star badges on players using the mod
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveUsers() {
        try {
            Instant cutoff = Instant.now().minus(ACTIVE_THRESHOLD);
            List<BadgeProfile> activeUsers = badgeProfileRepository.findPublishedActiveSince(cutoff);
            List<String> activeUuids = activeUsers.stream()
                .map(BadgeProfile::getUuid)
                .toList();
            List<BadgeInfo> badges = activeUsers.stream()
                .map(this::toBadgeInfo)
                .toList();

            return ResponseEntity.ok(Map.of(
                "uuids", activeUuids,
                "count", activeUuids.size(),
                "badges", badges
            ));
        } catch (Exception e) {
            logger.error("Error fetching active users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", "Error fetching active users"));
        }
    }

    /**
     * Get statistics about WynnExtras user base
     * GET /wynnextras-users/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            Instant cutoff = Instant.now().minus(ACTIVE_THRESHOLD);
            long activeCount = userRepository.countActiveUsersSince(cutoff);
            long totalCount = userRepository.count();
            long anonymousActiveCount = anonymousUserActivityRepository.countByLastSeenAtAfter(cutoff);
            long anonymousPeriodCount = anonymousUserActivityRepository.count();

            java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
            // Legacy fields intentionally retain their identified-only meaning.
            response.put("activeUsers", activeCount);
            response.put("totalUsers", totalCount);
            response.put("activeThresholdDays", ACTIVE_THRESHOLD.toDays());
            response.put("identified", java.util.Map.of(
                    "activeUsers", activeCount, "totalUsers", totalCount));
            response.put("anonymous", java.util.Map.of(
                    "activePeriodIds", anonymousActiveCount, "totalPeriodIds", anonymousPeriodCount));
            response.put("combined", java.util.Map.of(
                    "activeUsersAndPeriodIds", activeCount + anonymousActiveCount,
                    "totalUsersAndPeriodIds", totalCount + anonymousPeriodCount));
            response.put("versions", buildVersionStats(cutoff));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching user stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", "Error fetching stats"));
        }
    }

    private java.util.Map<String, Object> buildVersionStats(Instant cutoff) {
        java.util.Map<String, Long> identified = new java.util.TreeMap<>();
        for (WynnExtrasUserRepository.VersionUsage usage : userRepository.findVersionUsageSince(cutoff)) {
            if (usage.getModVersion() != null) identified.put(usage.getModVersion(), usage.getUserCount());
        }
        java.util.Map<String, Long> anonymous = new java.util.TreeMap<>();
        for (AnonymousUserActivityRepository.VersionUsage usage : anonymousUserActivityRepository.findVersionUsageSince(cutoff)) {
            if (usage.getModVersion() != null) anonymous.put(usage.getModVersion(), usage.getUserCount());
        }
        java.util.Map<String, Long> combined = new java.util.TreeMap<>(identified);
        anonymous.forEach((version, count) -> combined.merge(version, count, Long::sum));
        return java.util.Map.of("identified", identified, "anonymous", anonymous, "combined", combined);
    }

    /** Daily usage and retention with explicit identified/anonymous/combined series. */
    @GetMapping("/stats/activity")
    public ResponseEntity<?> getActivityStats() {
        try {
            return ResponseEntity.ok(java.util.Map.of(
                    "dailyActivity", buildDailyActivityStats(),
                    "retention", buildRetentionStats(),
                    "retentionNote", "Anonymous retention is limited to one 30-day identifier period"
            ));
        } catch (Exception e) {
            logger.error("Error fetching activity stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("status", "error", "message", "Error fetching activity stats"));
        }
    }

    /**
     * Compatibility endpoint for clients that still use the username fallback
     * when matching rendered player labels. It deliberately exposes only data
     * already present in the public active badge response.
     */
    @Deprecated
    @GetMapping("/active/details")
    public ResponseEntity<?> getLegacyActiveUserDetails() {
        try {
            Instant cutoff = Instant.now().minus(ACTIVE_THRESHOLD);
            List<LegacyActiveUserInfo> users = badgeProfileRepository.findPublishedActiveSince(cutoff).stream()
                    .map(user -> new LegacyActiveUserInfo(user.getUuid(), user.getUsername()))
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "users", users,
                    "count", users.size()
            ));
        } catch (Exception e) {
            logger.error("Error fetching legacy active user details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Error fetching active users"));
        }
    }

    private List<java.util.Map<String, Object>> buildDailyActivityStats() {
        java.util.Map<LocalDate, long[]> identified = dailyRows(dailyUserActivityRepository.findDailyHeartbeatStats());
        java.util.Map<LocalDate, long[]> anonymous = dailyRows(anonymousDailyActivityRepository.findDailyHeartbeatStats());
        java.util.Set<LocalDate> dates = new java.util.TreeSet<>(identified.keySet());
        dates.addAll(anonymous.keySet());
        return dates.stream().map(date -> {
            long[] i = identified.getOrDefault(date, new long[2]);
            long[] a = anonymous.getOrDefault(date, new long[2]);
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("date", date);
            row.put("identified", java.util.Map.of("uniqueUsers", i[0], "heartbeats", i[1]));
            row.put("anonymous", java.util.Map.of("uniquePeriodIds", a[0], "heartbeats", a[1]));
            row.put("combined", java.util.Map.of("uniqueUsersAndPeriodIds", i[0] + a[0], "heartbeats", i[1] + a[1]));
            return row;
        }).toList();
    }

    private List<java.util.Map<String, Object>> buildRetentionStats() {
        java.util.Map<LocalDate, long[]> identified = retentionRows(
                dailyUserActivityRepository.findFirstSeenCountsByDate(),
                dailyUserActivityRepository.findReturnedAfterSevenDayGapCountsByDate(),
                dailyUserActivityRepository.findDayOneRetentionCountsByDate());
        java.util.Map<LocalDate, long[]> anonymous = retentionRows(
                anonymousDailyActivityRepository.findFirstSeenCountsByDate(),
                anonymousDailyActivityRepository.findReturnedAfterSevenDayGapCountsByDate(),
                anonymousDailyActivityRepository.findDayOneRetentionCountsByDate());
        java.util.Set<LocalDate> dates = new java.util.TreeSet<>(identified.keySet());
        dates.addAll(anonymous.keySet());
        return dates.stream().map(date -> {
            long[] i = identified.getOrDefault(date, new long[3]);
            long[] a = anonymous.getOrDefault(date, new long[3]);
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("date", date);
            row.put("identified", retentionValues(i));
            row.put("anonymous", retentionValues(a));
            row.put("combined", retentionValues(new long[]{i[0] + a[0], i[1] + a[1], i[2] + a[2]}));
            return row;
        }).toList();
    }

    private static java.util.Map<LocalDate, long[]> dailyRows(List<Object[]> rows) {
        java.util.Map<LocalDate, long[]> result = new java.util.HashMap<>();
        for (Object[] row : rows) {
            result.put(toLocalDate(row[0]), new long[]{
                    ((Number) row[1]).longValue(), ((Number) row[2]).longValue()});
        }
        return result;
    }

    @SafeVarargs
    private static java.util.Map<LocalDate, long[]> retentionRows(List<Object[]>... metrics) {
        java.util.Map<LocalDate, long[]> result = new java.util.HashMap<>();
        for (int index = 0; index < metrics.length; index++) {
            for (Object[] row : metrics[index]) {
                result.computeIfAbsent(toLocalDate(row[0]), ignored -> new long[3])[index] =
                        ((Number) row[1]).longValue();
            }
        }
        return result;
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof java.util.Date date) {
            return LocalDate.ofInstant(date.toInstant(), ZoneOffset.UTC);
        }
        if (value instanceof CharSequence text) {
            return LocalDate.parse(text);
        }
        throw new IllegalArgumentException("Unsupported SQL date value: "
                + (value == null ? "null" : value.getClass().getName()));
    }

    private static java.util.Map<String, Long> retentionValues(long[] values) {
        return java.util.Map.of(
                "firstSeen", values[0],
                "returnedAfterSevenDayGap", values[1],
                "dayOneRetained", values[2]);
    }

    // Request/Response DTOs

    public static class HeartbeatRequest {
        private String modVersion;
        private String badgeIconId;
        private String badgeColorId;

        public String getModVersion() { return modVersion; }
        public void setModVersion(String modVersion) { this.modVersion = modVersion; }

        public String getBadgeIconId() { return badgeIconId; }
        public void setBadgeIconId(String badgeIconId) { this.badgeIconId = badgeIconId; }

        public String getBadgeColorId() { return badgeColorId; }
        public void setBadgeColorId(String badgeColorId) { this.badgeColorId = badgeColorId; }
    }

    public static class AnonymousHeartbeatRequest {
        private String anonymousId;
        private long period;
        private String modVersion;
        private final java.util.Map<String, Object> unknownFields = new HashMap<>();

        public String getAnonymousId() { return anonymousId; }
        public void setAnonymousId(String anonymousId) { this.anonymousId = anonymousId; }
        public long getPeriod() { return period; }
        public void setPeriod(long period) { this.period = period; }
        public String getModVersion() { return modVersion; }
        public void setModVersion(String modVersion) { this.modVersion = modVersion; }

        @JsonAnySetter
        public void setUnknownField(String name, Object value) { unknownFields.put(name, value); }
        public boolean hasUnknownFields() { return !unknownFields.isEmpty(); }
    }

    public static class BadgeUpdateRequest {
        private String badgeIconId;
        private String badgeColorId;
        private Boolean published;

        public String getBadgeIconId() { return badgeIconId; }
        public void setBadgeIconId(String badgeIconId) { this.badgeIconId = badgeIconId; }
        public String getBadgeColorId() { return badgeColorId; }
        public void setBadgeColorId(String badgeColorId) { this.badgeColorId = badgeColorId; }
        public Boolean getPublished() { return published; }
        public void setPublished(Boolean published) { this.published = published; }
    }

    public static class BadgeInfo {
        private String uuid;
        private String username;
        private String iconId;
        private String colorId;

        public BadgeInfo(String uuid, String username, String iconId, String colorId) {
            this.uuid = uuid;
            this.username = username;
            this.iconId = iconId;
            this.colorId = colorId;
        }

        public String getUuid() { return uuid; }
        public String getUsername() { return username; }
        public String getIconId() { return iconId; }
        public String getColorId() { return colorId; }
    }

    public static class LegacyActiveUserInfo {
        private final String uuid;
        private final String username;

        public LegacyActiveUserInfo(String uuid, String username) {
            this.uuid = uuid;
            this.username = username;
        }

        public String getUuid() { return uuid; }
        public String getUsername() { return username; }
    }

    private BadgeInfo toBadgeInfo(BadgeProfile user) {
        return new BadgeInfo(
                user.getUuid(),
                user.getUsername(),
                BadgeCatalog.normalizeBadgeIconId(user.getBadgeIconId()),
                BadgeCatalog.normalizeBadgeColorId(user.getBadgeColorId())
        );
    }

    // Helper Map class for response building
    private static class Map<K, V> extends HashMap<K, V> {
        public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
            Map<K, V> map = new Map<>();
            map.put(k1, v1);
            map.put(k2, v2);
            return map;
        }

        public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
            Map<K, V> map = new Map<>();
            map.put(k1, v1);
            map.put(k2, v2);
            map.put(k3, v3);
            return map;
        }
    }
}

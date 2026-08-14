package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * One pseudonymous installation identifier within one 30-day period.
 * This table deliberately has no relationship to an identified user.
 */
@Entity
@Table(name = "anonymous_user_activity", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"anonymous_id", "activity_period"})
}, indexes = {
        @Index(name = "idx_anonymous_activity_last_seen", columnList = "last_seen_at"),
        @Index(name = "idx_anonymous_activity_version", columnList = "mod_version")
})
public class AnonymousUserActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anonymous_id", nullable = false, length = 64)
    private String anonymousId;

    @Column(name = "activity_period", nullable = false)
    private long period;

    @Column(name = "mod_version", nullable = false, length = 32)
    private String modVersion;

    @Column(name = "heartbeat_count", nullable = false)
    private long heartbeatCount;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    public AnonymousUserActivity() {}

    public AnonymousUserActivity(String anonymousId, long period, String modVersion, Instant heartbeatAt) {
        this.anonymousId = anonymousId;
        this.period = period;
        this.modVersion = modVersion;
        this.heartbeatCount = 1;
        this.firstSeenAt = heartbeatAt;
        this.lastSeenAt = heartbeatAt;
    }

    public void recordHeartbeat(String modVersion, Instant heartbeatAt) {
        this.modVersion = modVersion;
        this.heartbeatCount++;
        this.lastSeenAt = heartbeatAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAnonymousId() { return anonymousId; }
    public void setAnonymousId(String anonymousId) { this.anonymousId = anonymousId; }
    public long getPeriod() { return period; }
    public void setPeriod(long period) { this.period = period; }
    public String getModVersion() { return modVersion; }
    public void setModVersion(String modVersion) { this.modVersion = modVersion; }
    public long getHeartbeatCount() { return heartbeatCount; }
    public void setHeartbeatCount(long heartbeatCount) { this.heartbeatCount = heartbeatCount; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(Instant firstSeenAt) { this.firstSeenAt = firstSeenAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}

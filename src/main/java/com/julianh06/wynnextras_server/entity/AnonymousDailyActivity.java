package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

/** Daily anonymous telemetry, kept separate from UUID-based activity. */
@Entity
@Table(name = "anonymous_daily_activity", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"activity_date", "activity_period", "anonymous_id"})
}, indexes = {
        @Index(name = "idx_anonymous_daily_id_date", columnList = "anonymous_id,activity_period,activity_date")
})
public class AnonymousDailyActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "activity_period", nullable = false)
    private long period;

    @Column(name = "anonymous_id", nullable = false, length = 32)
    private String anonymousId;

    @Column(name = "mod_version", nullable = false, length = 32)
    private String modVersion;

    @Column(name = "heartbeat_count", nullable = false)
    private long heartbeatCount;

    @Column(name = "first_heartbeat_at", nullable = false)
    private Instant firstHeartbeatAt;

    @Column(name = "last_heartbeat_at", nullable = false)
    private Instant lastHeartbeatAt;

    public AnonymousDailyActivity() {}

    public AnonymousDailyActivity(LocalDate activityDate, long period, String anonymousId,
                                  String modVersion, Instant heartbeatAt) {
        this.activityDate = activityDate;
        this.period = period;
        this.anonymousId = anonymousId;
        this.modVersion = modVersion;
        this.heartbeatCount = 1;
        this.firstHeartbeatAt = heartbeatAt;
        this.lastHeartbeatAt = heartbeatAt;
    }

    public void recordHeartbeat(String modVersion, Instant heartbeatAt) {
        this.modVersion = modVersion;
        this.heartbeatCount++;
        this.lastHeartbeatAt = heartbeatAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getActivityDate() { return activityDate; }
    public void setActivityDate(LocalDate activityDate) { this.activityDate = activityDate; }
    public long getPeriod() { return period; }
    public void setPeriod(long period) { this.period = period; }
    public String getAnonymousId() { return anonymousId; }
    public void setAnonymousId(String anonymousId) { this.anonymousId = anonymousId; }
    public String getModVersion() { return modVersion; }
    public void setModVersion(String modVersion) { this.modVersion = modVersion; }
    public long getHeartbeatCount() { return heartbeatCount; }
    public void setHeartbeatCount(long heartbeatCount) { this.heartbeatCount = heartbeatCount; }
    public Instant getFirstHeartbeatAt() { return firstHeartbeatAt; }
    public void setFirstHeartbeatAt(Instant firstHeartbeatAt) { this.firstHeartbeatAt = firstHeartbeatAt; }
    public Instant getLastHeartbeatAt() { return lastHeartbeatAt; }
    public void setLastHeartbeatAt(Instant lastHeartbeatAt) { this.lastHeartbeatAt = lastHeartbeatAt; }
}

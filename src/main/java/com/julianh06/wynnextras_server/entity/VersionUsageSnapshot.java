package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "version_usage_snapshot", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"snapshot_date", "mod_version"})
})
public class VersionUsageSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(nullable = false)
    private Instant capturedAt;

    @Column(name = "mod_version", nullable = false, length = 32)
    private String modVersion;

    @Column(nullable = false)
    private long userCount;

    private Long active1dCount = 0L;

    private Long active3dCount = 0L;

    @Column(nullable = false)
    private long active7dCount;

    @Column(nullable = false)
    private long active14dCount;

    @Column(columnDefinition = "bigint default 0")
    private Long anonymousUserCount = 0L;
    @Column(columnDefinition = "bigint default 0")
    private Long anonymousActive1dCount = 0L;
    @Column(columnDefinition = "bigint default 0")
    private Long anonymousActive3dCount = 0L;
    @Column(columnDefinition = "bigint default 0")
    private Long anonymousActive7dCount = 0L;
    @Column(columnDefinition = "bigint default 0")
    private Long anonymousActive14dCount = 0L;

    public VersionUsageSnapshot() {}

    public VersionUsageSnapshot(LocalDate snapshotDate, Instant capturedAt, String modVersion) {
        this.snapshotDate = snapshotDate;
        this.capturedAt = capturedAt;
        this.modVersion = modVersion;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }

    public Instant getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Instant capturedAt) { this.capturedAt = capturedAt; }

    public String getModVersion() { return modVersion; }
    public void setModVersion(String modVersion) { this.modVersion = modVersion; }

    public long getUserCount() { return userCount; }
    public void setUserCount(long userCount) { this.userCount = userCount; }

    public long getActive1dCount() { return active1dCount == null ? 0L : active1dCount; }
    public void setActive1dCount(long active1dCount) { this.active1dCount = active1dCount; }

    public long getActive3dCount() { return active3dCount == null ? 0L : active3dCount; }
    public void setActive3dCount(long active3dCount) { this.active3dCount = active3dCount; }

    public long getActive7dCount() { return active7dCount; }
    public void setActive7dCount(long active7dCount) { this.active7dCount = active7dCount; }

    public long getActive14dCount() { return active14dCount; }
    public void setActive14dCount(long active14dCount) { this.active14dCount = active14dCount; }

    public long getAnonymousUserCount() { return value(anonymousUserCount); }
    public void setAnonymousUserCount(long value) { this.anonymousUserCount = value; }
    public long getAnonymousActive1dCount() { return value(anonymousActive1dCount); }
    public void setAnonymousActive1dCount(long value) { this.anonymousActive1dCount = value; }
    public long getAnonymousActive3dCount() { return value(anonymousActive3dCount); }
    public void setAnonymousActive3dCount(long value) { this.anonymousActive3dCount = value; }
    public long getAnonymousActive7dCount() { return value(anonymousActive7dCount); }
    public void setAnonymousActive7dCount(long value) { this.anonymousActive7dCount = value; }
    public long getAnonymousActive14dCount() { return value(anonymousActive14dCount); }
    public void setAnonymousActive14dCount(long value) { this.anonymousActive14dCount = value; }

    public long getCombinedUserCount() { return userCount + getAnonymousUserCount(); }
    public long getCombinedActive1dCount() { return getActive1dCount() + getAnonymousActive1dCount(); }
    public long getCombinedActive3dCount() { return getActive3dCount() + getAnonymousActive3dCount(); }
    public long getCombinedActive7dCount() { return active7dCount + getAnonymousActive7dCount(); }
    public long getCombinedActive14dCount() { return active14dCount + getAnonymousActive14dCount(); }

    private static long value(Long value) { return value == null ? 0L : value; }
}

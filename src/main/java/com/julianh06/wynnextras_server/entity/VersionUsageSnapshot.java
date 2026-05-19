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
}

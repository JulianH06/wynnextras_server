package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "wynncraft_usage_snapshot", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"snapshot_date"})
})
public class WynncraftUsageSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "unique_players", nullable = false)
    private long uniquePlayers;

    @Column(name = "wynnextras_users", nullable = false)
    private long wynnExtrasUsers;

    @Column(name = "usage_percent", nullable = false)
    private double usagePercent;

    @Column(name = "sample_count", nullable = false)
    private long sampleCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public WynncraftUsageSnapshot() {}

    public WynncraftUsageSnapshot(LocalDate snapshotDate, Instant capturedAt) {
        this.snapshotDate = snapshotDate;
        this.capturedAt = capturedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }

    public Instant getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Instant capturedAt) { this.capturedAt = capturedAt; }

    public long getUniquePlayers() { return uniquePlayers; }
    public void setUniquePlayers(long uniquePlayers) { this.uniquePlayers = uniquePlayers; }

    public long getWynnExtrasUsers() { return wynnExtrasUsers; }
    public void setWynnExtrasUsers(long wynnExtrasUsers) { this.wynnExtrasUsers = wynnExtrasUsers; }

    public double getUsagePercent() { return usagePercent; }
    public void setUsagePercent(double usagePercent) { this.usagePercent = usagePercent; }

    public long getSampleCount() { return sampleCount; }
    public void setSampleCount(long sampleCount) { this.sampleCount = sampleCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "active_user_snapshot", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"snapshot_date"})
})
public class ActiveUserSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(nullable = false)
    private Instant capturedAt;

    @Column(nullable = false)
    private long active1d;

    @Column(nullable = false)
    private long active3d;

    @Column(nullable = false)
    private long active5d;

    @Column(nullable = false)
    private long active7d;

    @Column(nullable = false)
    private long active10d;

    @Column(nullable = false)
    private long active14d;

    public ActiveUserSnapshot() {}

    public ActiveUserSnapshot(LocalDate snapshotDate, Instant capturedAt) {
        this.snapshotDate = snapshotDate;
        this.capturedAt = capturedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }

    public Instant getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Instant capturedAt) { this.capturedAt = capturedAt; }

    public long getActive1d() { return active1d; }
    public void setActive1d(long active1d) { this.active1d = active1d; }

    public long getActive3d() { return active3d; }
    public void setActive3d(long active3d) { this.active3d = active3d; }

    public long getActive5d() { return active5d; }
    public void setActive5d(long active5d) { this.active5d = active5d; }

    public long getActive7d() { return active7d; }
    public void setActive7d(long active7d) { this.active7d = active7d; }

    public long getActive10d() { return active10d; }
    public void setActive10d(long active10d) { this.active10d = active10d; }

    public long getActive14d() { return active14d; }
    public void setActive14d(long active14d) { this.active14d = active14d; }
}

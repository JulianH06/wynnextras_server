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

    @Column(columnDefinition = "bigint default 0")
    private Long anonymousActive1d = 0L;
    @Column(columnDefinition = "bigint default 0")
    private Long anonymousActive3d = 0L;
    @Column(columnDefinition = "bigint default 0")
    private Long anonymousActive5d = 0L;
    @Column(columnDefinition = "bigint default 0")
    private Long anonymousActive7d = 0L;
    @Column(columnDefinition = "bigint default 0")
    private Long anonymousActive10d = 0L;
    @Column(columnDefinition = "bigint default 0")
    private Long anonymousActive14d = 0L;

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

    public long getAnonymousActive1d() { return value(anonymousActive1d); }
    public void setAnonymousActive1d(long value) { this.anonymousActive1d = value; }
    public long getAnonymousActive3d() { return value(anonymousActive3d); }
    public void setAnonymousActive3d(long value) { this.anonymousActive3d = value; }
    public long getAnonymousActive5d() { return value(anonymousActive5d); }
    public void setAnonymousActive5d(long value) { this.anonymousActive5d = value; }
    public long getAnonymousActive7d() { return value(anonymousActive7d); }
    public void setAnonymousActive7d(long value) { this.anonymousActive7d = value; }
    public long getAnonymousActive10d() { return value(anonymousActive10d); }
    public void setAnonymousActive10d(long value) { this.anonymousActive10d = value; }
    public long getAnonymousActive14d() { return value(anonymousActive14d); }
    public void setAnonymousActive14d(long value) { this.anonymousActive14d = value; }

    public long getCombinedActive1d() { return active1d + getAnonymousActive1d(); }
    public long getCombinedActive3d() { return active3d + getAnonymousActive3d(); }
    public long getCombinedActive5d() { return active5d + getAnonymousActive5d(); }
    public long getCombinedActive7d() { return active7d + getAnonymousActive7d(); }
    public long getCombinedActive10d() { return active10d + getAnonymousActive10d(); }
    public long getCombinedActive14d() { return active14d + getAnonymousActive14d(); }

    private static long value(Long value) { return value == null ? 0L : value; }
}

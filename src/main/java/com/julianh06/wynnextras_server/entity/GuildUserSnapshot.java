package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "guild_user_snapshot", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"snapshot_date", "guild_tag"})
})
public class GuildUserSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(nullable = false)
    private Instant capturedAt;

    @Column(name = "guild_tag", nullable = false, length = 16)
    private String guildTag;

    @Column(length = 100)
    private String guildName;

    @Column(nullable = false)
    private int memberCount;

    @Column(nullable = false)
    private int wynnExtrasUsersTotal;

    @Column(nullable = false)
    private int active1d;

    @Column(nullable = false)
    private int active3d;

    @Column(nullable = false)
    private int active5d;

    @Column(nullable = false)
    private int active7d;

    @Column(nullable = false)
    private int active10d;

    @Column(nullable = false)
    private int active14d;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public GuildUserSnapshot() {}

    public GuildUserSnapshot(LocalDate snapshotDate, Instant capturedAt, String guildTag) {
        this.snapshotDate = snapshotDate;
        this.capturedAt = capturedAt;
        this.guildTag = guildTag;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }

    public Instant getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Instant capturedAt) { this.capturedAt = capturedAt; }

    public String getGuildTag() { return guildTag; }
    public void setGuildTag(String guildTag) { this.guildTag = guildTag; }

    public String getGuildName() { return guildName; }
    public void setGuildName(String guildName) { this.guildName = guildName; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public int getWynnExtrasUsersTotal() { return wynnExtrasUsersTotal; }
    public void setWynnExtrasUsersTotal(int wynnExtrasUsersTotal) { this.wynnExtrasUsersTotal = wynnExtrasUsersTotal; }

    public int getActive1d() { return active1d; }
    public void setActive1d(int active1d) { this.active1d = active1d; }

    public int getActive3d() { return active3d; }
    public void setActive3d(int active3d) { this.active3d = active3d; }

    public int getActive5d() { return active5d; }
    public void setActive5d(int active5d) { this.active5d = active5d; }

    public int getActive7d() { return active7d; }
    public void setActive7d(int active7d) { this.active7d = active7d; }

    public int getActive10d() { return active10d; }
    public void setActive10d(int active10d) { this.active10d = active10d; }

    public int getActive14d() { return active14d; }
    public void setActive14d(int active14d) { this.active14d = active14d; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

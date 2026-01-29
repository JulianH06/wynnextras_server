package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Stores player metadata for the achievement system.
 * Tracks aggregate stats like total points, achievement counts, etc.
 */
@Entity
@Table(name = "achievement_player")
public class AchievementPlayer {
    @Id
    @Column(nullable = false, length = 36)
    private String uuid; // Minecraft UUID (no dashes, lowercase)

    @Column(nullable = false, length = 32)
    private String playerName; // Current Minecraft username

    @Column(nullable = false)
    private int totalPoints; // Achievement points (Bronze=1, Silver=2, Gold=3, non-tiered=1)

    @Column(nullable = false)
    private int unlockedCount; // Total achievements unlocked

    @Column(nullable = false)
    private int goldCount; // Number of gold tier achievements

    @Column(nullable = false)
    private int silverCount; // Number of silver tier achievements

    @Column(nullable = false)
    private int bronzeCount; // Number of bronze tier achievements

    @Column(nullable = false, length = 32)
    private String modVersion; // WynnExtras version

    @Column(nullable = false)
    private Instant lastSyncedAt; // When achievements were last synced

    @Column(nullable = false)
    private Instant createdAt; // When player first synced achievements

    public AchievementPlayer() {}

    public AchievementPlayer(String uuid, String playerName, String modVersion) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.modVersion = modVersion;
        this.totalPoints = 0;
        this.unlockedCount = 0;
        this.goldCount = 0;
        this.silverCount = 0;
        this.bronzeCount = 0;
        this.lastSyncedAt = Instant.now();
        this.createdAt = Instant.now();
    }

    // Getters and setters
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

    public int getUnlockedCount() { return unlockedCount; }
    public void setUnlockedCount(int unlockedCount) { this.unlockedCount = unlockedCount; }

    public int getGoldCount() { return goldCount; }
    public void setGoldCount(int goldCount) { this.goldCount = goldCount; }

    public int getSilverCount() { return silverCount; }
    public void setSilverCount(int silverCount) { this.silverCount = silverCount; }

    public int getBronzeCount() { return bronzeCount; }
    public void setBronzeCount(int bronzeCount) { this.bronzeCount = bronzeCount; }

    public String getModVersion() { return modVersion; }
    public void setModVersion(String modVersion) { this.modVersion = modVersion; }

    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    /**
     * Recalculate aggregate stats from achievements
     */
    public void recalculateStats(int unlocked, int bronze, int silver, int gold) {
        this.unlockedCount = unlocked;
        this.bronzeCount = bronze;
        this.silverCount = silver;
        this.goldCount = gold;
        // Points: Bronze=1, Silver=2, Gold=3, non-tiered unlocked=1
        this.totalPoints = bronze + (silver * 2) + (gold * 3);
    }
}

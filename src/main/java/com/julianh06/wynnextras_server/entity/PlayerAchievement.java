package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "player_achievement", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"player_uuid", "achievement_id"})
})
public class PlayerAchievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String playerUuid;

    @Column(nullable = false)
    private String playerName;

    @Column(name = "achievement_id", nullable = false)
    private String achievementId;

    @Column(nullable = false)
    private boolean unlocked;

    @Column(nullable = false)
    private int currentProgress;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(length = 50)
    private String modVersion;

    public PlayerAchievement() {}

    public PlayerAchievement(String playerUuid, String playerName, String achievementId, boolean unlocked,
                             int currentProgress, String modVersion) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.achievementId = achievementId;
        this.unlocked = unlocked;
        this.currentProgress = currentProgress;
        this.modVersion = modVersion;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getAchievementId() { return achievementId; }
    public void setAchievementId(String achievementId) { this.achievementId = achievementId; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public int getCurrentProgress() { return currentProgress; }
    public void setCurrentProgress(int currentProgress) { this.currentProgress = currentProgress; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getModVersion() { return modVersion; }
    public void setModVersion(String modVersion) { this.modVersion = modVersion; }
}

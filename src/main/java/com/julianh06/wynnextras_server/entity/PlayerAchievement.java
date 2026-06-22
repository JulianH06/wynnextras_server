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
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false)
    private boolean secret;

    @Column(nullable = false)
    private boolean unlocked;

    private Instant unlockedAt;

    @Column(nullable = false)
    private int currentProgress;

    private Integer targetProgress;

    private Integer currentLevel;

    @Column(length = 1000)
    private String levelTargetsJson;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(length = 50)
    private String modVersion;

    public PlayerAchievement() {}

    public PlayerAchievement(String playerUuid, String playerName, String achievementId, String title, String description,
                             String type, boolean secret, boolean unlocked, Instant unlockedAt, int currentProgress,
                             Integer targetProgress, Integer currentLevel, String levelTargetsJson, String modVersion) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.achievementId = achievementId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.secret = secret;
        this.unlocked = unlocked;
        this.unlockedAt = unlockedAt;
        this.currentProgress = currentProgress;
        this.targetProgress = targetProgress;
        this.currentLevel = currentLevel;
        this.levelTargetsJson = levelTargetsJson;
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

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isSecret() { return secret; }
    public void setSecret(boolean secret) { this.secret = secret; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public Instant getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(Instant unlockedAt) { this.unlockedAt = unlockedAt; }

    public int getCurrentProgress() { return currentProgress; }
    public void setCurrentProgress(int currentProgress) { this.currentProgress = currentProgress; }

    public Integer getTargetProgress() { return targetProgress; }
    public void setTargetProgress(Integer targetProgress) { this.targetProgress = targetProgress; }

    public Integer getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(Integer currentLevel) { this.currentLevel = currentLevel; }

    public String getLevelTargetsJson() { return levelTargetsJson; }
    public void setLevelTargetsJson(String levelTargetsJson) { this.levelTargetsJson = levelTargetsJson; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getModVersion() { return modVersion; }
    public void setModVersion(String modVersion) { this.modVersion = modVersion; }
}

package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Stores a player's progress on a specific achievement.
 *
 * Achievement tiers: NONE (non-tiered), BRONZE, SILVER, GOLD
 * Categories: RAIDING, ASPECTS, MISC, PROFESSIONS, WARS, LOOTRUNS
 */
@Entity
@Table(
    name = "player_achievement",
    uniqueConstraints = @UniqueConstraint(columnNames = {"player_uuid", "achievement_id"})
)
public class PlayerAchievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_uuid", nullable = false, length = 36)
    private String playerUuid; // Minecraft UUID (no dashes, lowercase)

    @Column(name = "achievement_id", nullable = false, length = 64)
    private String achievementId; // e.g., "raid_tna_completions", "aspect_max_mythic"

    @Column(nullable = false, length = 32)
    private String category; // RAIDING, ASPECTS, MISC, PROFESSIONS, WARS, LOOTRUNS

    @Column(nullable = false)
    private int progress; // Current progress count (e.g., 75 out of 100)

    @Column(length = 10)
    private String tier; // BRONZE, SILVER, GOLD, or null for non-tiered achievements

    @Column(nullable = false)
    private boolean unlocked; // Whether the achievement is unlocked

    @Column
    private Instant unlockedAt; // When the achievement was first unlocked

    @Column
    private Instant tierUpgradedAt; // When the tier was last upgraded

    @Column(nullable = false)
    private Instant updatedAt; // Last time this achievement was synced

    public PlayerAchievement() {}

    public PlayerAchievement(String playerUuid, String achievementId, String category) {
        this.playerUuid = playerUuid;
        this.achievementId = achievementId;
        this.category = category;
        this.progress = 0;
        this.tier = null;
        this.unlocked = false;
        this.updatedAt = Instant.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }

    public String getAchievementId() { return achievementId; }
    public void setAchievementId(String achievementId) { this.achievementId = achievementId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public Instant getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(Instant unlockedAt) { this.unlockedAt = unlockedAt; }

    public Instant getTierUpgradedAt() { return tierUpgradedAt; }
    public void setTierUpgradedAt(Instant tierUpgradedAt) { this.tierUpgradedAt = tierUpgradedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

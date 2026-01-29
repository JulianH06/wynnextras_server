package com.julianh06.wynnextras_server.dto;

import java.util.List;

/**
 * DTO for syncing achievements from client to server.
 *
 * The client sends the full list of achievements with their current state.
 * The server merges this with existing data.
 */
public class AchievementSyncDto {
    private String modVersion;
    private List<AchievementDto> achievements;

    public AchievementSyncDto() {}

    public String getModVersion() { return modVersion; }
    public void setModVersion(String modVersion) { this.modVersion = modVersion; }

    public List<AchievementDto> getAchievements() { return achievements; }
    public void setAchievements(List<AchievementDto> achievements) { this.achievements = achievements; }

    /**
     * Individual achievement data
     */
    public static class AchievementDto {
        private String id;           // Achievement ID (e.g., "raid_tna_100")
        private String category;     // RAIDING, ASPECTS, MISC, PROFESSIONS, WARS, LOOTRUNS
        private int progress;        // Current progress count
        private String tier;         // BRONZE, SILVER, GOLD, or null
        private boolean unlocked;    // Whether unlocked
        private Long unlockedAt;     // Epoch millis when first unlocked (null if not unlocked)
        private Long tierUpgradedAt; // Epoch millis when tier last changed (null if no tier)

        public AchievementDto() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }

        public String getTier() { return tier; }
        public void setTier(String tier) { this.tier = tier; }

        public boolean isUnlocked() { return unlocked; }
        public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

        public Long getUnlockedAt() { return unlockedAt; }
        public void setUnlockedAt(Long unlockedAt) { this.unlockedAt = unlockedAt; }

        public Long getTierUpgradedAt() { return tierUpgradedAt; }
        public void setTierUpgradedAt(Long tierUpgradedAt) { this.tierUpgradedAt = tierUpgradedAt; }
    }
}

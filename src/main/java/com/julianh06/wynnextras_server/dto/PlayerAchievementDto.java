package com.julianh06.wynnextras_server.dto;

import java.util.List;

public class PlayerAchievementDto {

    public static class AchievementData {
        private String id;
        private String title;
        private String description;
        private String type;
        private boolean secret;
        private boolean unlocked;
        private Long unlockedAt;
        private int current;
        private Integer target;
        private Integer currentLevel;
        private List<Integer> levelTargets;

        public AchievementData() {}

        public AchievementData(String id, String title, String description, String type, boolean secret, boolean unlocked,
                               Long unlockedAt, int current, Integer target, Integer currentLevel, List<Integer> levelTargets) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.type = type;
            this.secret = secret;
            this.unlocked = unlocked;
            this.unlockedAt = unlockedAt;
            this.current = current;
            this.target = target;
            this.currentLevel = currentLevel;
            this.levelTargets = levelTargets;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

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

        public Long getUnlockedAt() { return unlockedAt; }
        public void setUnlockedAt(Long unlockedAt) { this.unlockedAt = unlockedAt; }

        public int getCurrent() { return current; }
        public void setCurrent(int current) { this.current = current; }

        public Integer getTarget() { return target; }
        public void setTarget(Integer target) { this.target = target; }

        public Integer getCurrentLevel() { return currentLevel; }
        public void setCurrentLevel(Integer currentLevel) { this.currentLevel = currentLevel; }

        public List<Integer> getLevelTargets() { return levelTargets; }
        public void setLevelTargets(List<Integer> levelTargets) { this.levelTargets = levelTargets; }
    }

    public static class UploadRequest {
        private String modVersion;
        private List<AchievementData> achievements;

        public UploadRequest() {}

        public String getModVersion() { return modVersion; }
        public void setModVersion(String modVersion) { this.modVersion = modVersion; }

        public List<AchievementData> getAchievements() { return achievements; }
        public void setAchievements(List<AchievementData> achievements) { this.achievements = achievements; }
    }

    public static class PlayerAchievementsResponse {
        private String playerUuid;
        private String playerName;
        private String modVersion;
        private long updatedAt;
        private long unlockedCount;
        private List<AchievementData> achievements;

        public PlayerAchievementsResponse() {}

        public PlayerAchievementsResponse(String playerUuid, String playerName, String modVersion, long updatedAt,
                                          long unlockedCount, List<AchievementData> achievements) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.modVersion = modVersion;
            this.updatedAt = updatedAt;
            this.unlockedCount = unlockedCount;
            this.achievements = achievements;
        }

        public String getPlayerUuid() { return playerUuid; }
        public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }

        public String getModVersion() { return modVersion; }
        public void setModVersion(String modVersion) { this.modVersion = modVersion; }

        public long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

        public long getUnlockedCount() { return unlockedCount; }
        public void setUnlockedCount(long unlockedCount) { this.unlockedCount = unlockedCount; }

        public List<AchievementData> getAchievements() { return achievements; }
        public void setAchievements(List<AchievementData> achievements) { this.achievements = achievements; }
    }

    public static class LeaderboardEntry {
        private String playerUuid;
        private String playerName;
        private long achievementCount;
        private long lastUpdated;

        public LeaderboardEntry() {}

        public LeaderboardEntry(String playerUuid, String playerName, long achievementCount, long lastUpdated) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.achievementCount = achievementCount;
            this.lastUpdated = lastUpdated;
        }

        public String getPlayerUuid() { return playerUuid; }
        public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }

        public long getAchievementCount() { return achievementCount; }
        public void setAchievementCount(long achievementCount) { this.achievementCount = achievementCount; }

        public long getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    public static class PlayerListEntry {
        private String playerUuid;
        private String playerName;
        private String modVersion;
        private long lastUpdated;
        private long achievementCount;

        public PlayerListEntry() {}

        public PlayerListEntry(String playerUuid, String playerName, String modVersion, long lastUpdated, long achievementCount) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.modVersion = modVersion;
            this.lastUpdated = lastUpdated;
            this.achievementCount = achievementCount;
        }

        public String getPlayerUuid() { return playerUuid; }
        public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }

        public String getModVersion() { return modVersion; }
        public void setModVersion(String modVersion) { this.modVersion = modVersion; }

        public long getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

        public long getAchievementCount() { return achievementCount; }
        public void setAchievementCount(long achievementCount) { this.achievementCount = achievementCount; }
    }
}

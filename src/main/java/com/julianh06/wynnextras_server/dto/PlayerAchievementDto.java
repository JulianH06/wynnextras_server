package com.julianh06.wynnextras_server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public class PlayerAchievementDto {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AchievementState {
        private String id;
        private Boolean unlocked;
        private Integer current;

        public AchievementState() {}

        public AchievementState(String id, Boolean unlocked, Integer current) {
            this.id = id;
            this.unlocked = unlocked;
            this.current = current;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public Boolean getUnlocked() { return unlocked; }
        public void setUnlocked(Boolean unlocked) { this.unlocked = unlocked; }

        public Integer getCurrent() { return current; }
        public void setCurrent(Integer current) { this.current = current; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AchievementData {
        private String id;
        private boolean unlocked;
        private int current;

        public AchievementData() {}

        public AchievementData(String id, boolean unlocked, int current) {
            this.id = id;
            this.unlocked = unlocked;
            this.current = current;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public boolean isUnlocked() { return unlocked; }
        public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

        public int getCurrent() { return current; }
        public void setCurrent(int current) { this.current = current; }
    }

    public static class UploadRequest {
        private Integer schemaVersion;
        private String modVersion;
        private List<AchievementState> achievements;

        public UploadRequest() {}

        public Integer getSchemaVersion() { return schemaVersion; }
        public void setSchemaVersion(Integer schemaVersion) { this.schemaVersion = schemaVersion; }

        public String getModVersion() { return modVersion; }
        public void setModVersion(String modVersion) { this.modVersion = modVersion; }

        public List<AchievementState> getAchievements() { return achievements; }
        public void setAchievements(List<AchievementState> achievements) { this.achievements = achievements; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
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

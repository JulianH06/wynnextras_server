package com.julianh06.wynnextras_server.dto;

import java.util.List;

public class PersonalAspectDto {

    public static class AspectData {
        private String name;
        private String rarity;
        private int amount;

        public AspectData() {}

        public AspectData(String name, String rarity, int amount) {
            this.name = name;
            this.rarity = rarity;
            this.amount = amount;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getRarity() { return rarity; }
        public void setRarity(String rarity) { this.rarity = rarity; }

        public int getAmount() { return amount; }
        public void setAmount(int amount) { this.amount = amount; }
    }

    public static class UploadRequest {
        private String playerName;
        private String modVersion;
        private List<AspectData> aspects;

        // Optional fields for backward compatibility with old mod versions
        private String uuid;
        private Long updatedAt;

        public UploadRequest() {}

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }

        public String getModVersion() { return modVersion; }
        public void setModVersion(String modVersion) { this.modVersion = modVersion; }

        public List<AspectData> getAspects() { return aspects; }
        public void setAspects(List<AspectData> aspects) { this.aspects = aspects; }

        // Backward compatibility getters/setters
        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }

        public Long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class PlayerAspectsResponse {
        private String playerUuid;
        private String playerName;
        private String modVersion;
        private long updatedAt;
        private List<AspectData> aspects;

        public PlayerAspectsResponse() {}

        public PlayerAspectsResponse(String playerUuid, String playerName, String modVersion, long updatedAt, List<AspectData> aspects) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.modVersion = modVersion;
            this.updatedAt = updatedAt;
            this.aspects = aspects;
        }

        public String getPlayerUuid() { return playerUuid; }
        public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }

        public String getModVersion() { return modVersion; }
        public void setModVersion(String modVersion) { this.modVersion = modVersion; }

        public long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

        public List<AspectData> getAspects() { return aspects; }
        public void setAspects(List<AspectData> aspects) { this.aspects = aspects; }
    }

    public static class LeaderboardEntry {
        private String playerUuid;
        private String playerName;
        private long maxAspectCount;

        public LeaderboardEntry() {}

        public LeaderboardEntry(String playerUuid, String playerName, long maxAspectCount) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.maxAspectCount = maxAspectCount;
        }

        public String getPlayerUuid() { return playerUuid; }
        public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }

        public long getMaxAspectCount() { return maxAspectCount; }
        public void setMaxAspectCount(long maxAspectCount) { this.maxAspectCount = maxAspectCount; }
    }

    public static class PlayerListEntry {
        private String playerUuid;
        private String playerName;
        private String modVersion;
        private long lastUpdated;
        private long aspectCount;

        public PlayerListEntry() {}

        public PlayerListEntry(String playerUuid, String playerName, String modVersion, long lastUpdated, long aspectCount) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.modVersion = modVersion;
            this.lastUpdated = lastUpdated;
            this.aspectCount = aspectCount;
        }

        public String getPlayerUuid() { return playerUuid; }
        public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }

        public String getModVersion() { return modVersion; }
        public void setModVersion(String modVersion) { this.modVersion = modVersion; }

        public long getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

        public long getAspectCount() { return aspectCount; }
        public void setAspectCount(long aspectCount) { this.aspectCount = aspectCount; }
    }
}

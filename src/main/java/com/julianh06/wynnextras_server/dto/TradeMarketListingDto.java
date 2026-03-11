package com.julianh06.wynnextras_server.dto;

import java.util.List;

public class TradeMarketListingDto {

    // POST body wrapper
    public static class BatchSubmission {
        private List<ListingData> listings;

        public BatchSubmission() {}

        public List<ListingData> getListings() { return listings; }
        public void setListings(List<ListingData> listings) { this.listings = listings; }
    }

    // Individual listing data (used for both input and output)
    public static class ListingData {
        private String name;
        private String rarity;
        private String itemType;
        private String type;
        private long listingPrice;
        private double overallPercentage;
        private List<StatRoll> stats;
        private String shinyStat;
        private long timestamp;

        public ListingData() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getRarity() { return rarity; }
        public void setRarity(String rarity) { this.rarity = rarity; }

        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public long getListingPrice() { return listingPrice; }
        public void setListingPrice(long listingPrice) { this.listingPrice = listingPrice; }

        public double getOverallPercentage() { return overallPercentage; }
        public void setOverallPercentage(double overallPercentage) { this.overallPercentage = overallPercentage; }

        public List<StatRoll> getStats() { return stats; }
        public void setStats(List<StatRoll> stats) { this.stats = stats; }

        public String getShinyStat() { return shinyStat; }
        public void setShinyStat(String shinyStat) { this.shinyStat = shinyStat; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    public static class StatRoll {
        private String name;
        private String value;
        private double percentage;

        public StatRoll() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
    }
}

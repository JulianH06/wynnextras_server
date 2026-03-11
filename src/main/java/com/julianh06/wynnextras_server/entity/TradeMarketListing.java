package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "trade_market_listing")
public class TradeMarketListing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String submittedBy;

    @Column(nullable = false)
    private String submitterUuid;

    @Column(nullable = false)
    private String itemName;

    @Column(nullable = false)
    private String rarity;

    private String itemType;

    private String type;

    @Column(nullable = false)
    private long listingPrice;

    private Double overallPct;

    @Column(columnDefinition = "TEXT")
    private String statsJson;

    private String shinyStat;

    private Long clientTimestamp;

    @Column(nullable = false)
    private Instant submittedAt;

    public TradeMarketListing() {}

    public TradeMarketListing(String submittedBy, String submitterUuid, String itemName, String rarity,
                              String itemType, String type, long listingPrice, Double overallPct,
                              String statsJson, String shinyStat, Long clientTimestamp) {
        this.submittedBy = submittedBy;
        this.submitterUuid = submitterUuid;
        this.itemName = itemName;
        this.rarity = rarity;
        this.itemType = itemType;
        this.type = type;
        this.listingPrice = listingPrice;
        this.overallPct = overallPct;
        this.statsJson = statsJson;
        this.shinyStat = shinyStat;
        this.clientTimestamp = clientTimestamp;
        this.submittedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    public String getSubmitterUuid() { return submitterUuid; }
    public void setSubmitterUuid(String submitterUuid) { this.submitterUuid = submitterUuid; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getRarity() { return rarity; }
    public void setRarity(String rarity) { this.rarity = rarity; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getListingPrice() { return listingPrice; }
    public void setListingPrice(long listingPrice) { this.listingPrice = listingPrice; }

    public Double getOverallPct() { return overallPct; }
    public void setOverallPct(Double overallPct) { this.overallPct = overallPct; }

    public String getStatsJson() { return statsJson; }
    public void setStatsJson(String statsJson) { this.statsJson = statsJson; }

    public String getShinyStat() { return shinyStat; }
    public void setShinyStat(String shinyStat) { this.shinyStat = shinyStat; }

    public Long getClientTimestamp() { return clientTimestamp; }
    public void setClientTimestamp(Long clientTimestamp) { this.clientTimestamp = clientTimestamp; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
}

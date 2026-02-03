package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "lootrun_lootpool_submission")
public class LootrunLootPoolSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String lootrunType; // SE, SI, MH, CORK, COTL

    @Column(nullable = false)
    private String submittedBy; // username

    @Column(nullable = false, columnDefinition = "TEXT")
    private String itemsJson; // JSON string of sorted items

    @Column(nullable = false)
    private Instant submittedAt;

    @Column(nullable = false, length = 10)
    private String weekIdentifier; // e.g., "2026-W04"

    public LootrunLootPoolSubmission() {}

    public LootrunLootPoolSubmission(String lootrunType, String submittedBy, String itemsJson, String weekIdentifier) {
        this.lootrunType = lootrunType;
        this.submittedBy = submittedBy;
        this.itemsJson = itemsJson;
        this.weekIdentifier = weekIdentifier;
        this.submittedAt = Instant.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLootrunType() { return lootrunType; }
    public void setLootrunType(String lootrunType) { this.lootrunType = lootrunType; }

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    public String getItemsJson() { return itemsJson; }
    public void setItemsJson(String itemsJson) { this.itemsJson = itemsJson; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public String getWeekIdentifier() { return weekIdentifier; }
    public void setWeekIdentifier(String weekIdentifier) { this.weekIdentifier = weekIdentifier; }
}

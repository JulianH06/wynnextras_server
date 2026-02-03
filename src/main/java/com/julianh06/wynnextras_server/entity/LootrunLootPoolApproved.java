package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "lootrun_lootpool_approved")
public class LootrunLootPoolApproved {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String lootrunType; // SE, SI, MH, CORK, COTL

    @Column(nullable = false, columnDefinition = "TEXT")
    private String itemsJson; // JSON string of approved items

    @Column(nullable = false)
    private Instant approvedAt;

    @Column(nullable = false, length = 10)
    private String weekIdentifier; // e.g., "2026-W04"

    @Column(nullable = false)
    private boolean locked; // locked at 10 submissions

    @Column(nullable = false)
    private int submissionCount; // how many submissions matched

    public LootrunLootPoolApproved() {}

    public LootrunLootPoolApproved(String lootrunType, String itemsJson, String weekIdentifier, int submissionCount, boolean locked) {
        this.lootrunType = lootrunType;
        this.itemsJson = itemsJson;
        this.weekIdentifier = weekIdentifier;
        this.submissionCount = submissionCount;
        this.locked = locked;
        this.approvedAt = Instant.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLootrunType() { return lootrunType; }
    public void setLootrunType(String lootrunType) { this.lootrunType = lootrunType; }

    public String getItemsJson() { return itemsJson; }
    public void setItemsJson(String itemsJson) { this.itemsJson = itemsJson; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }

    public String getWeekIdentifier() { return weekIdentifier; }
    public void setWeekIdentifier(String weekIdentifier) { this.weekIdentifier = weekIdentifier; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public int getSubmissionCount() { return submissionCount; }
    public void setSubmissionCount(int submissionCount) { this.submissionCount = submissionCount; }
}

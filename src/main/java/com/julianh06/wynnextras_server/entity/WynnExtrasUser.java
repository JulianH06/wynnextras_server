package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Tracks WynnExtras mod users for the user badge (⭐) system.
 * Users who have been active within the last 7 days are considered "active"
 * and will have their star badge displayed.
 */
@Entity
@Table(name = "wynnextras_user")
public class WynnExtrasUser {
    @Id
    @Column(nullable = false, length = 36)
    private String uuid; // Minecraft UUID (primary identifier, no dashes, lowercase)

    @Column(nullable = false, length = 32)
    private String username; // Current Minecraft username

    @Column(nullable = false)
    private Instant lastSeen; // When the user was last active

    @Column(nullable = false, length = 32)
    private String modVersion; // Version of WynnExtras they're using

    @Column(nullable = false)
    private Instant createdAt; // When user first registered

    public WynnExtrasUser() {}

    public WynnExtrasUser(String uuid, String username, String modVersion) {
        this.uuid = uuid;
        this.username = username;
        this.modVersion = modVersion;
        this.lastSeen = Instant.now();
        this.createdAt = Instant.now();
    }

    // Getters and setters
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }

    public String getModVersion() { return modVersion; }
    public void setModVersion(String modVersion) { this.modVersion = modVersion; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    /**
     * Check if this user is considered active (last seen within 7 days)
     */
    public boolean isActive() {
        return lastSeen.isAfter(Instant.now().minus(java.time.Duration.ofDays(7)));
    }
}

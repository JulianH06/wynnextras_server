package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Account-level aspect visibility, including accounts with no aspect rows yet. */
@Entity
@Table(name = "aspect_publication_preference")
public class AspectPublicationPreference {
    @Id
    @Column(name = "player_uuid", nullable = false, length = 36)
    private String playerUuid;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AspectPublicationPreference() {}

    public AspectPublicationPreference(String playerUuid, boolean published, Instant updatedAt) {
        this.playerUuid = playerUuid;
        this.published = published;
        this.updatedAt = updatedAt;
    }

    public String getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

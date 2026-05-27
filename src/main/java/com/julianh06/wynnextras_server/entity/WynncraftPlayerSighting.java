package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "wynncraft_player_sighting", indexes = {
        @Index(name = "idx_wynncraft_sighting_sampled_at", columnList = "sampled_at"),
        @Index(name = "idx_wynncraft_sighting_player_uuid", columnList = "player_uuid")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"player_uuid", "sampled_at"})
})
public class WynncraftPlayerSighting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_uuid", nullable = false, length = 32)
    private String playerUuid;

    @Column(name = "sampled_at", nullable = false)
    private Instant sampledAt;

    public WynncraftPlayerSighting() {}

    public WynncraftPlayerSighting(String playerUuid, Instant sampledAt) {
        this.playerUuid = playerUuid;
        this.sampledAt = sampledAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }

    public Instant getSampledAt() { return sampledAt; }
    public void setSampledAt(Instant sampledAt) { this.sampledAt = sampledAt; }
}
package com.julianh06.wynnextras_server.entity;

import com.julianh06.wynnextras_server.util.BadgeCatalog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/** Public badge state, deliberately independent from identified usage activity. */
@Entity
@Table(name = "badge_profile", indexes = {
        @Index(name = "idx_badge_profile_published_last_seen", columnList = "published,badge_last_seen")
})
public class BadgeProfile {
    @Id
    @Column(name = "player_uuid", nullable = false, length = 36)
    private String uuid;

    @Column(nullable = false, length = 32)
    private String username;

    @Column(name = "badge_icon_id", nullable = false, length = 32)
    private String badgeIconId = BadgeCatalog.DEFAULT_BADGE_ICON_ID;

    @Column(name = "badge_color_id", nullable = false, length = 32)
    private String badgeColorId = BadgeCatalog.DEFAULT_BADGE_COLOR_ID;

    @Column(nullable = false)
    private boolean published = true;

    @Column(name = "badge_last_seen", nullable = false)
    private Instant badgeLastSeen;

    public BadgeProfile() {}

    public BadgeProfile(String uuid, String username, Instant badgeLastSeen) {
        this.uuid = uuid;
        this.username = username;
        this.badgeLastSeen = badgeLastSeen;
    }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getBadgeIconId() { return badgeIconId; }
    public void setBadgeIconId(String badgeIconId) { this.badgeIconId = badgeIconId; }
    public String getBadgeColorId() { return badgeColorId; }
    public void setBadgeColorId(String badgeColorId) { this.badgeColorId = badgeColorId; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public Instant getBadgeLastSeen() { return badgeLastSeen; }
    public void setBadgeLastSeen(Instant badgeLastSeen) { this.badgeLastSeen = badgeLastSeen; }
}

package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.BadgeProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BadgeProfileRepository extends JpaRepository<BadgeProfile, String> {
    @Modifying
    @Query(value = """
            INSERT INTO badge_profile
                (player_uuid, username, badge_icon_id, badge_color_id, published, badge_last_seen)
            VALUES
                (:uuid, :username, :iconId, :colorId, true, :badgeLastSeen)
            ON CONFLICT (player_uuid) DO UPDATE SET
                username = EXCLUDED.username,
                badge_icon_id = CASE
                    WHEN badge_profile.published THEN EXCLUDED.badge_icon_id
                    ELSE badge_profile.badge_icon_id
                END,
                badge_color_id = CASE
                    WHEN badge_profile.published THEN EXCLUDED.badge_color_id
                    ELSE badge_profile.badge_color_id
                END,
                badge_last_seen = EXCLUDED.badge_last_seen
            """, nativeQuery = true)
    void upsertFromHeartbeat(@Param("uuid") String uuid,
                             @Param("username") String username,
                             @Param("iconId") String iconId,
                             @Param("colorId") String colorId,
                             @Param("badgeLastSeen") Instant badgeLastSeen);

    @Modifying
    @Query(value = """
            INSERT INTO badge_profile
                (player_uuid, username, badge_icon_id, badge_color_id, published, badge_last_seen)
            VALUES
                (:uuid, :username, :iconId, :colorId, :published, :badgeLastSeen)
            ON CONFLICT (player_uuid) DO UPDATE SET
                badge_icon_id = EXCLUDED.badge_icon_id,
                badge_color_id = EXCLUDED.badge_color_id,
                published = EXCLUDED.published,
                badge_last_seen = EXCLUDED.badge_last_seen
            """, nativeQuery = true)
    void upsertFromBadgeUpdate(@Param("uuid") String uuid,
                               @Param("username") String username,
                               @Param("iconId") String iconId,
                               @Param("colorId") String colorId,
                               @Param("published") boolean published,
                               @Param("badgeLastSeen") Instant badgeLastSeen);

    @Modifying
    @Query(value = """
            INSERT INTO badge_profile
                (player_uuid, username, badge_icon_id, badge_color_id, published, badge_last_seen)
            VALUES
                (:uuid, :username, :defaultIconId, :defaultColorId, false, :badgeLastSeen)
            ON CONFLICT (player_uuid) DO UPDATE SET
                published = false
            """, nativeQuery = true)
    void hideBadge(@Param("uuid") String uuid,
                   @Param("username") String username,
                   @Param("defaultIconId") String defaultIconId,
                   @Param("defaultColorId") String defaultColorId,
                   @Param("badgeLastSeen") Instant badgeLastSeen);

    @Query("SELECT b FROM BadgeProfile b WHERE b.badgeLastSeen > :cutoff AND b.published = true")
    List<BadgeProfile> findPublishedActiveSince(@Param("cutoff") Instant cutoff);

    @Query("""
            SELECT b.badgeIconId AS badgeIconId, COUNT(b) AS usageCount
            FROM BadgeProfile b
            WHERE b.badgeLastSeen > :cutoff AND b.published = true
            GROUP BY b.badgeIconId
            ORDER BY COUNT(b) DESC, b.badgeIconId ASC
            """)
    List<BadgeIconUsage> findBadgeIconUsage(@Param("cutoff") Instant cutoff);

    @Query("""
            SELECT b.badgeColorId AS badgeColorId, COUNT(b) AS usageCount
            FROM BadgeProfile b
            WHERE b.badgeLastSeen > :cutoff AND b.published = true
            GROUP BY b.badgeColorId
            ORDER BY COUNT(b) DESC, b.badgeColorId ASC
            """)
    List<BadgeColorUsage> findBadgeColorUsage(@Param("cutoff") Instant cutoff);

    @Query("""
            SELECT b.badgeIconId AS badgeIconId, b.badgeColorId AS badgeColorId, COUNT(b) AS usageCount
            FROM BadgeProfile b
            WHERE b.badgeLastSeen > :cutoff AND b.published = true
            GROUP BY b.badgeIconId, b.badgeColorId
            ORDER BY COUNT(b) DESC, b.badgeIconId ASC, b.badgeColorId ASC
            """)
    List<BadgeCombinationUsage> findBadgeCombinationUsage(@Param("cutoff") Instant cutoff);

    interface BadgeIconUsage {
        String getBadgeIconId();
        long getUsageCount();
    }

    interface BadgeColorUsage {
        String getBadgeColorId();
        long getUsageCount();
    }

    interface BadgeCombinationUsage {
        String getBadgeIconId();
        String getBadgeColorId();
        long getUsageCount();
    }
}

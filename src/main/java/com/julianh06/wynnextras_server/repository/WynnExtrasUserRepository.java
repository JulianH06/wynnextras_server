package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.WynnExtrasUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WynnExtrasUserRepository extends JpaRepository<WynnExtrasUser, String> {

    @Modifying
    @Query(value = """
            INSERT INTO wynnextras_user
                (uuid, username, last_seen, mod_version, badge_icon_id, badge_color_id,
                 badge_published, created_at)
            VALUES
                (:uuid, :username, :heartbeatAt, :modVersion, :defaultIconId, :defaultColorId,
                 true, :heartbeatAt)
            ON CONFLICT (uuid) DO UPDATE SET
                username = EXCLUDED.username,
                last_seen = EXCLUDED.last_seen,
                mod_version = EXCLUDED.mod_version
            """, nativeQuery = true)
    void upsertHeartbeat(@Param("uuid") String uuid,
                         @Param("username") String username,
                         @Param("modVersion") String modVersion,
                         @Param("defaultIconId") String defaultIconId,
                         @Param("defaultColorId") String defaultColorId,
                         @Param("heartbeatAt") Instant heartbeatAt);

    /**
     * Find user by UUID
     */
    Optional<WynnExtrasUser> findByUuid(String uuid);

    /**
     * Find user by username (case-insensitive)
     */
    Optional<WynnExtrasUser> findByUsernameIgnoreCase(String username);

    /**
     * Get all users who have been active since the given cutoff time
     */
    @Query("SELECT u FROM WynnExtrasUser u WHERE u.lastSeen > :cutoff")
    List<WynnExtrasUser> findActiveUsersSince(@Param("cutoff") Instant cutoff);

    /** Public badge feed: hidden badge selections must not leak through UUIDs or badge data. */
    @Query("SELECT u FROM WynnExtrasUser u WHERE u.lastSeen > :cutoff AND u.badgePublished = true")
    List<WynnExtrasUser> findPublishedActiveUsersSince(@Param("cutoff") Instant cutoff);

    /**
     * Minimal data set needed to render the dashboard charts.  In particular,
     * badge data and usernames are not loaded for every user on each /db visit.
     */
    @Query("""
            SELECT u.createdAt AS createdAt, u.lastSeen AS lastSeen, u.modVersion AS modVersion
            FROM WynnExtrasUser u
            WHERE u.lastSeen > :cutoff
            """)
    List<DbDashboardUser> findDbDashboardUsers(@Param("cutoff") Instant cutoff);

    @Query("""
            SELECT u.badgeIconId AS badgeIconId, COUNT(u) AS usageCount
            FROM WynnExtrasUser u
            WHERE u.lastSeen > :cutoff AND u.badgePublished = true
            GROUP BY u.badgeIconId
            ORDER BY COUNT(u) DESC, u.badgeIconId ASC
            """)
    List<BadgeIconUsage> findBadgeIconUsage(@Param("cutoff") Instant cutoff);

    @Query("""
            SELECT u.badgeColorId AS badgeColorId, COUNT(u) AS usageCount
            FROM WynnExtrasUser u
            WHERE u.lastSeen > :cutoff AND u.badgePublished = true
            GROUP BY u.badgeColorId
            ORDER BY COUNT(u) DESC, u.badgeColorId ASC
            """)
    List<BadgeColorUsage> findBadgeColorUsage(@Param("cutoff") Instant cutoff);

    @Query("""
            SELECT u.badgeIconId AS badgeIconId, u.badgeColorId AS badgeColorId, COUNT(u) AS usageCount
            FROM WynnExtrasUser u
            WHERE u.lastSeen > :cutoff AND u.badgePublished = true
            GROUP BY u.badgeIconId, u.badgeColorId
            ORDER BY COUNT(u) DESC, u.badgeIconId ASC, u.badgeColorId ASC
            """)
    List<BadgeCombinationUsage> findBadgeCombinationUsage(@Param("cutoff") Instant cutoff);

    /**
     * Get just the UUIDs of active users (more efficient for the badge list)
     */
    @Query("SELECT u.uuid FROM WynnExtrasUser u WHERE u.lastSeen > :cutoff")
    List<String> findActiveUuidsSince(@Param("cutoff") Instant cutoff);

    /**
     * Count active users
     */
    @Query("SELECT COUNT(u) FROM WynnExtrasUser u WHERE u.lastSeen > :cutoff")
    long countActiveUsersSince(@Param("cutoff") Instant cutoff);

    @Query("""
            SELECT u.modVersion AS modVersion, COUNT(u) AS userCount
            FROM WynnExtrasUser u
            WHERE u.lastSeen > :cutoff
            GROUP BY u.modVersion
            ORDER BY u.modVersion
            """)
    List<VersionUsage> findVersionUsageSince(@Param("cutoff") Instant cutoff);

    /**
     * Lightweight, paginated projection for the database dashboard's user list.
     * Keeping this separate from the dashboard aggregates avoids serializing every
     * player into the initial HTML response.
     */
    @Query("""
            SELECT u.uuid AS uuid, u.username AS username, u.createdAt AS createdAt,
                   u.lastSeen AS lastSeen, u.modVersion AS modVersion
            FROM WynnExtrasUser u
            ORDER BY u.createdAt ASC, u.uuid ASC
            """)
    Slice<DbUserListEntry> findDbUserList(Pageable pageable);

    interface DbUserListEntry {
        String getUuid();
        String getUsername();
        Instant getCreatedAt();
        Instant getLastSeen();
        String getModVersion();
    }

    interface DbDashboardUser {
        Instant getCreatedAt();
        Instant getLastSeen();
        String getModVersion();
    }

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

    interface VersionUsage {
        String getModVersion();
        long getUserCount();
    }
}

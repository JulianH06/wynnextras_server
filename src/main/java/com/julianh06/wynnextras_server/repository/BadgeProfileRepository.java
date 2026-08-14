package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.BadgeProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BadgeProfileRepository extends JpaRepository<BadgeProfile, String> {
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

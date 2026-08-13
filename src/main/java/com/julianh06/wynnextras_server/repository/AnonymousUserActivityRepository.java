package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.AnonymousUserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnonymousUserActivityRepository extends JpaRepository<AnonymousUserActivity, Long> {
    Optional<AnonymousUserActivity> findByAnonymousIdAndPeriod(String anonymousId, long period);

    long countByLastSeenAtAfter(Instant cutoff);

    @Query("""
        SELECT a.modVersion AS modVersion, COUNT(a) AS userCount
        FROM AnonymousUserActivity a
        WHERE a.lastSeenAt > :cutoff
        GROUP BY a.modVersion
        ORDER BY a.modVersion
        """)
    List<VersionUsage> findVersionUsageSince(@Param("cutoff") Instant cutoff);

    interface VersionUsage {
        String getModVersion();
        long getUserCount();
    }
}

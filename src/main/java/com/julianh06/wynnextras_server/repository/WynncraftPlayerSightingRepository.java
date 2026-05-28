package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.WynncraftPlayerSighting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface WynncraftPlayerSightingRepository extends JpaRepository<WynncraftPlayerSighting, Long> {
    @Query("SELECT COUNT(DISTINCT s.sampledAt) FROM WynncraftPlayerSighting s WHERE s.sampledAt >= :start AND s.sampledAt < :end")
    long countSamplesBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            SELECT COUNT(DISTINCT s.playerUuid)
            FROM WynncraftPlayerSighting s
            WHERE s.sampledAt >= :start
              AND s.sampledAt < :end
              AND s.playerUuid IN (
                SELECT d.userUuid
                FROM DailyUserActivity d
                WHERE d.activityDate = :activityDate
              )
            """)
    long countDailyActiveWynnExtrasUsersSeenBetween(
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("activityDate") LocalDate activityDate);

    @Query("""
            SELECT COUNT(DISTINCT s.playerUuid)
            FROM WynncraftPlayerSighting s
            WHERE s.sampledAt >= :start
              AND s.sampledAt < :end
            """)
    long countUniquePlayersSeenInRange(@Param("start") Instant start, @Param("end") Instant end);

    @Query(value = """
            SELECT s.sampled_at AS "sampledAt",
                   COUNT(DISTINCT s.player_uuid) AS "visiblePlayers",
                   COUNT(DISTINCT CASE WHEN EXISTS (
                       SELECT 1
                       FROM daily_user_activity d
                       WHERE d.user_uuid = s.player_uuid
                         AND d.first_heartbeat_at <= s.sampled_at
                         AND d.last_heartbeat_at >= s.sampled_at - INTERVAL '24 hours'
                   ) THEN s.player_uuid END) AS "wynnExtrasUsers"
            FROM wynncraft_player_sighting s
            WHERE s.sampled_at >= :start
              AND s.sampled_at < :end
            GROUP BY s.sampled_at
            ORDER BY s.sampled_at ASC
            """, nativeQuery = true)
    List<UsageSampleBreakdownRow> findUsageSampleBreakdownBetween(
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Modifying
    @Query("DELETE FROM WynncraftPlayerSighting s WHERE s.sampledAt < :cutoff")
    int deleteBySampledAtBefore(@Param("cutoff") Instant cutoff);

    interface UsageSampleBreakdownRow {
        Instant getSampledAt();
        long getVisiblePlayers();
        long getWynnExtrasUsers();
    }
}
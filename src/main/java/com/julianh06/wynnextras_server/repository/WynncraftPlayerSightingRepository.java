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

    @Query("""
            SELECT s.sampledAt AS sampledAt,
                   COUNT(DISTINCT s.playerUuid) AS visiblePlayers,
                   COUNT(DISTINCT d.userUuid) AS wynnExtrasUsers
            FROM WynncraftPlayerSighting s
            LEFT JOIN DailyUserActivity d
              ON d.activityDate = :activityDate
             AND d.userUuid = s.playerUuid
            WHERE s.sampledAt >= :start
              AND s.sampledAt < :end
            GROUP BY s.sampledAt
            ORDER BY s.sampledAt ASC
            """)
    List<UsageSampleBreakdownRow> findUsageSampleBreakdownBetween(
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("activityDate") LocalDate activityDate);

    @Modifying
    @Query("DELETE FROM WynncraftPlayerSighting s WHERE s.sampledAt < :cutoff")
    int deleteBySampledAtBefore(@Param("cutoff") Instant cutoff);

    interface UsageSampleBreakdownRow {
        Instant getSampledAt();
        long getVisiblePlayers();
        long getWynnExtrasUsers();
    }
}
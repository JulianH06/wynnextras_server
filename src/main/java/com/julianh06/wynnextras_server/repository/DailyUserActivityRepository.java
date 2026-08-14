package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.DailyUserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyUserActivityRepository extends JpaRepository<DailyUserActivity, Long> {
    @Modifying
    @Query(value = """
        INSERT INTO daily_user_activity
            (activity_date, user_uuid, username, mod_version, heartbeat_count,
             first_heartbeat_at, last_heartbeat_at)
        VALUES
            (:activityDate, :userUuid, :username, :modVersion, 1, :heartbeatAt, :heartbeatAt)
        ON CONFLICT (activity_date, user_uuid) DO UPDATE SET
            username = EXCLUDED.username,
            mod_version = EXCLUDED.mod_version,
            heartbeat_count = daily_user_activity.heartbeat_count + 1,
            last_heartbeat_at = EXCLUDED.last_heartbeat_at
        """, nativeQuery = true)
    void upsertHeartbeat(@Param("activityDate") LocalDate activityDate,
                         @Param("userUuid") String userUuid,
                         @Param("username") String username,
                         @Param("modVersion") String modVersion,
                         @Param("heartbeatAt") java.time.Instant heartbeatAt);

    Optional<DailyUserActivity> findByActivityDateAndUserUuid(LocalDate activityDate, String userUuid);

    @Query("""
        SELECT d.activityDate, COUNT(d), COALESCE(SUM(d.heartbeatCount), 0)
        FROM DailyUserActivity d
        GROUP BY d.activityDate
        ORDER BY d.activityDate ASC
        """)
    List<Object[]> findDailyHeartbeatStats();

    @Query("""
        SELECT COUNT(d)
        FROM DailyUserActivity d
        WHERE d.activityDate = :activityDate
          AND d.userUuid NOT IN (
              SELECT prev.userUuid FROM DailyUserActivity prev WHERE prev.activityDate < :activityDate
          )
        """)
    long countFirstSeenOnDate(@Param("activityDate") LocalDate activityDate);

    @Query("""
        SELECT COUNT(d)
        FROM DailyUserActivity d
        WHERE d.activityDate = :activityDate
          AND d.userUuid IN (
              SELECT old.userUuid FROM DailyUserActivity old WHERE old.activityDate < :returnCutoff
          )
          AND d.userUuid NOT IN (
              SELECT recent.userUuid
              FROM DailyUserActivity recent
              WHERE recent.activityDate >= :returnCutoff
                AND recent.activityDate < :activityDate
          )
        """)
    long countReturnedAfterGap(@Param("activityDate") LocalDate activityDate, @Param("returnCutoff") LocalDate returnCutoff);

    @Query("""
        SELECT COUNT(d)
        FROM DailyUserActivity d
        WHERE d.activityDate = :cohortDate
          AND EXISTS (
              SELECT later.id
              FROM DailyUserActivity later
              WHERE later.userUuid = d.userUuid
                AND later.activityDate = :returnDate
          )
        """)
    long countCohortReturnedOnDate(@Param("cohortDate") LocalDate cohortDate, @Param("returnDate") LocalDate returnDate);

    /** Aggregated replacement for one countFirstSeenOnDate query per day. */
    @Query(value = """
        SELECT first_seen.activity_date, COUNT(*)
        FROM (
            SELECT user_uuid, MIN(activity_date) AS activity_date
            FROM daily_user_activity
            GROUP BY user_uuid
        ) first_seen
        GROUP BY first_seen.activity_date
        ORDER BY first_seen.activity_date ASC
        """, nativeQuery = true)
    List<Object[]> findFirstSeenCountsByDate();

    /**
     * Users whose last previous activity was more than seven days ago.
     * A window function lets PostgreSQL calculate every day in one table scan.
     */
    @Query(value = """
        SELECT activity_date, COUNT(*)
        FROM (
            SELECT activity_date,
                   LAG(activity_date) OVER (PARTITION BY user_uuid ORDER BY activity_date) AS previous_activity_date
            FROM daily_user_activity
        ) activity
        WHERE previous_activity_date < activity_date - 7
        GROUP BY activity_date
        ORDER BY activity_date ASC
        """, nativeQuery = true)
    List<Object[]> findReturnedAfterSevenDayGapCountsByDate();

    /** Aggregated replacement for one D1-retention query per day. */
    @Query(value = """
        SELECT current_activity.activity_date, COUNT(*)
        FROM daily_user_activity current_activity
        INNER JOIN daily_user_activity cohort
            ON cohort.user_uuid = current_activity.user_uuid
           AND cohort.activity_date = current_activity.activity_date - 1
        GROUP BY current_activity.activity_date
        ORDER BY current_activity.activity_date ASC
        """, nativeQuery = true)
    List<Object[]> findDayOneRetentionCountsByDate();
}

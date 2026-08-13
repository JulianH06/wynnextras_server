package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.AnonymousDailyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnonymousDailyActivityRepository extends JpaRepository<AnonymousDailyActivity, Long> {
    Optional<AnonymousDailyActivity> findByActivityDateAndPeriodAndAnonymousId(
            LocalDate activityDate, long period, String anonymousId);

    @Query("""
        SELECT d.activityDate, COUNT(d), COALESCE(SUM(d.heartbeatCount), 0)
        FROM AnonymousDailyActivity d
        GROUP BY d.activityDate
        ORDER BY d.activityDate ASC
        """)
    List<Object[]> findDailyHeartbeatStats();

    @Query(value = """
        SELECT first_seen.activity_date, COUNT(*)
        FROM (
            SELECT anonymous_id, activity_period, MIN(activity_date) AS activity_date
            FROM anonymous_daily_activity
            GROUP BY anonymous_id, activity_period
        ) first_seen
        GROUP BY first_seen.activity_date
        ORDER BY first_seen.activity_date ASC
        """, nativeQuery = true)
    List<Object[]> findFirstSeenCountsByDate();

    @Query(value = """
        SELECT activity_date, COUNT(*)
        FROM (
            SELECT activity_date,
                   LAG(activity_date) OVER (
                       PARTITION BY anonymous_id, activity_period ORDER BY activity_date
                   ) AS previous_activity_date
            FROM anonymous_daily_activity
        ) activity
        WHERE previous_activity_date < activity_date - 7
        GROUP BY activity_date
        ORDER BY activity_date ASC
        """, nativeQuery = true)
    List<Object[]> findReturnedAfterSevenDayGapCountsByDate();

    @Query(value = """
        SELECT current_activity.activity_date, COUNT(*)
        FROM anonymous_daily_activity current_activity
        INNER JOIN anonymous_daily_activity cohort
            ON cohort.anonymous_id = current_activity.anonymous_id
           AND cohort.activity_period = current_activity.activity_period
           AND cohort.activity_date = current_activity.activity_date - 1
        GROUP BY current_activity.activity_date
        ORDER BY current_activity.activity_date ASC
        """, nativeQuery = true)
    List<Object[]> findDayOneRetentionCountsByDate();
}

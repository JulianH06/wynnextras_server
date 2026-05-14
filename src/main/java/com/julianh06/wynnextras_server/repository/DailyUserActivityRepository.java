package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.DailyUserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyUserActivityRepository extends JpaRepository<DailyUserActivity, Long> {
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
}

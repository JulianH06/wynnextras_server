package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.AchievementPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementPlayerRepository extends JpaRepository<AchievementPlayer, String> {

    /**
     * Find player by UUID
     */
    Optional<AchievementPlayer> findByUuid(String uuid);

    /**
     * Find player by username (case-insensitive)
     */
    Optional<AchievementPlayer> findByPlayerNameIgnoreCase(String playerName);

    /**
     * Get leaderboard by total points
     */
    List<AchievementPlayer> findTop100ByOrderByTotalPointsDesc();

    /**
     * Get leaderboard with limit
     */
    @Query("SELECT p FROM AchievementPlayer p ORDER BY p.totalPoints DESC LIMIT :limit")
    List<AchievementPlayer> findTopByPoints(@Param("limit") int limit);

    /**
     * Get leaderboard by gold count
     */
    List<AchievementPlayer> findTop100ByOrderByGoldCountDesc();

    /**
     * Get leaderboard by unlocked count
     */
    List<AchievementPlayer> findTop100ByOrderByUnlockedCountDesc();

    /**
     * Count total players with achievements
     */
    long count();

    /**
     * Get all players (for listing)
     */
    List<AchievementPlayer> findAllByOrderByTotalPointsDesc();
}

package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.PlayerAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerAchievementRepository extends JpaRepository<PlayerAchievement, Long> {

    /**
     * Find all achievements for a player
     */
    List<PlayerAchievement> findByPlayerUuid(String playerUuid);

    /**
     * Find all achievements for a player in a specific category
     */
    List<PlayerAchievement> findByPlayerUuidAndCategory(String playerUuid, String category);

    /**
     * Find a specific achievement for a player
     */
    Optional<PlayerAchievement> findByPlayerUuidAndAchievementId(String playerUuid, String achievementId);

    /**
     * Find all unlocked achievements for a player
     */
    List<PlayerAchievement> findByPlayerUuidAndUnlockedTrue(String playerUuid);

    /**
     * Count unlocked achievements for a player
     */
    long countByPlayerUuidAndUnlockedTrue(String playerUuid);

    /**
     * Count achievements by tier for a player
     */
    @Query("SELECT COUNT(a) FROM PlayerAchievement a WHERE a.playerUuid = :uuid AND a.tier = :tier")
    long countByPlayerUuidAndTier(@Param("uuid") String playerUuid, @Param("tier") String tier);

    /**
     * Count non-tiered unlocked achievements for a player
     */
    @Query("SELECT COUNT(a) FROM PlayerAchievement a WHERE a.playerUuid = :uuid AND a.unlocked = true AND a.tier IS NULL")
    long countNonTieredUnlocked(@Param("uuid") String playerUuid);

    /**
     * Delete all achievements for a player (for full resync)
     */
    void deleteByPlayerUuid(String playerUuid);
}

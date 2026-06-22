package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.PlayerAchievement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerAchievementRepository extends JpaRepository<PlayerAchievement, Long> {
    List<PlayerAchievement> findByPlayerUuidOrderByAchievementIdAsc(String playerUuid);
    Optional<PlayerAchievement> findByPlayerUuidAndAchievementId(String playerUuid, String achievementId);
    void deleteByPlayerUuid(String playerUuid);

    @Query("""
        SELECT p.playerUuid, p.playerName, COUNT(p), MAX(p.updatedAt)
        FROM PlayerAchievement p
        WHERE p.unlocked = true
        GROUP BY p.playerUuid, p.playerName
        ORDER BY COUNT(p) DESC, MAX(p.updatedAt) ASC
        """)
    List<Object[]> findAchievementLeaderboard(Pageable pageable);

    @Query("""
        SELECT p.playerUuid, p.playerName, p.modVersion, MAX(p.updatedAt), COUNT(p)
        FROM PlayerAchievement p
        GROUP BY p.playerUuid, p.playerName, p.modVersion
        ORDER BY MAX(p.updatedAt) DESC
        """)
    List<Object[]> findAllPlayersWithAchievements();
}

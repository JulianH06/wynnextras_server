package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.PersonalAspect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalAspectRepository extends JpaRepository<PersonalAspect, Long> {
    List<PersonalAspect> findByPlayerUuid(String playerUuid);
    Optional<PersonalAspect> findByPlayerUuidAndAspectName(String playerUuid, String aspectName);
    void deleteByPlayerUuid(String playerUuid);

    /**
     * Get leaderboard of players with most max aspects (amount = 3)
     * Returns list of [playerUuid, playerName, maxAspectCount]
     */
    @Query("""
        SELECT p.playerUuid, p.playerName, COUNT(*) as maxCount
        FROM PersonalAspect p
        WHERE p.amount = 3
        GROUP BY p.playerUuid, p.playerName
        ORDER BY maxCount DESC
        LIMIT :limit
        """)
    List<Object[]> findTopPlayersByMaxAspects(int limit);

    /**
     * Get all players who have aspects in the database
     * Returns list of [playerUuid, playerName, modVersion, updatedAt, aspectCount]
     * Ordered by most recently updated first
     */
    @Query("""
        SELECT p.playerUuid, p.playerName, p.modVersion, MAX(p.updatedAt), COUNT(*)
        FROM PersonalAspect p
        GROUP BY p.playerUuid, p.playerName, p.modVersion
        ORDER BY MAX(p.updatedAt) DESC
        """)
    List<Object[]> findAllPlayersWithAspects();
}

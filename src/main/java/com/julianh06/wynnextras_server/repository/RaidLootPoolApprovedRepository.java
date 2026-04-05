package com.julianh06.wynnextras_server.repository;
import com.julianh06.wynnextras_server.entity.RaidLootPoolApproved;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RaidLootPoolApprovedRepository extends JpaRepository<RaidLootPoolApproved, Long> {
    Optional<RaidLootPoolApproved> findByRaidTypeAndWeekIdentifier(String raidType, String weekIdentifier);

    @Modifying
    @Query("DELETE FROM RaidLootPoolApproved r WHERE r.raidType = :raidType")
    long deleteByRaidType(String raidType);
}

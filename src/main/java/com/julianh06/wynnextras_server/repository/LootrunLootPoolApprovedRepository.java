package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.LootrunLootPoolApproved;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LootrunLootPoolApprovedRepository extends JpaRepository<LootrunLootPoolApproved, Long> {
    Optional<LootrunLootPoolApproved> findFirstByLootrunTypeAndWeekIdentifierOrderByApprovedAtDesc(String lootrunType, String weekIdentifier);
}

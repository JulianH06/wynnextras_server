package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.LootrunLootPoolSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LootrunLootPoolSubmissionRepository extends JpaRepository<LootrunLootPoolSubmission, Long> {
    List<LootrunLootPoolSubmission> findByLootrunTypeAndWeekIdentifier(String lootrunType, String weekIdentifier);
    List<LootrunLootPoolSubmission> findByWeekIdentifier(String weekIdentifier);
    List<LootrunLootPoolSubmission> findByLootrunTypeAndWeekIdentifierAndSubmittedByOrderBySubmittedAtDesc(
            String lootrunType, String weekIdentifier, String submittedBy);
}

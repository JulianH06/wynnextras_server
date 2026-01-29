package com.julianh06.wynnextras_server.repository;
import com.julianh06.wynnextras_server.entity.GambitSubmission;

import com.julianh06.wynnextras_server.entity.RaidLootPoolSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GambitSubmissionRepository extends JpaRepository<GambitSubmission, Long> {
    List<GambitSubmission> findByDayIdentifier(String dayIdentifier);
    List<GambitSubmission> findByDayIdentifierAndSubmittedByOrderBySubmittedAtDesc(String dayId, String submittedBy);
}

package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.ActiveUserSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActiveUserSnapshotRepository extends JpaRepository<ActiveUserSnapshot, Long> {
    Optional<ActiveUserSnapshot> findBySnapshotDate(LocalDate snapshotDate);
    List<ActiveUserSnapshot> findTop90ByOrderBySnapshotDateDesc();
}

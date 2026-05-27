package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.WynncraftUsageSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WynncraftUsageSnapshotRepository extends JpaRepository<WynncraftUsageSnapshot, Long> {
    Optional<WynncraftUsageSnapshot> findBySnapshotDate(LocalDate snapshotDate);
    List<WynncraftUsageSnapshot> findTop90ByOrderBySnapshotDateDesc();
}
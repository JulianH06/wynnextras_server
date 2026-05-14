package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.VersionUsageSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VersionUsageSnapshotRepository extends JpaRepository<VersionUsageSnapshot, Long> {
    Optional<VersionUsageSnapshot> findBySnapshotDateAndModVersion(LocalDate snapshotDate, String modVersion);
    List<VersionUsageSnapshot> findTop1000ByOrderBySnapshotDateAscModVersionAsc();
}

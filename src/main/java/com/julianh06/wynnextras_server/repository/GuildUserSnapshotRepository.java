package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.GuildUserSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GuildUserSnapshotRepository extends JpaRepository<GuildUserSnapshot, Long> {
    Optional<GuildUserSnapshot> findBySnapshotDateAndGuildTag(LocalDate snapshotDate, String guildTag);
    List<GuildUserSnapshot> findTop1000ByOrderBySnapshotDateAscGuildTagAsc();
}

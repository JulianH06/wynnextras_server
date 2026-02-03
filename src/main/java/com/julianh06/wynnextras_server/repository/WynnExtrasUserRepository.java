package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.WynnExtrasUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WynnExtrasUserRepository extends JpaRepository<WynnExtrasUser, String> {

    /**
     * Find user by UUID
     */
    Optional<WynnExtrasUser> findByUuid(String uuid);

    /**
     * Find user by username (case-insensitive)
     */
    Optional<WynnExtrasUser> findByUsernameIgnoreCase(String username);

    /**
     * Get all users who have been active since the given cutoff time
     */
    @Query("SELECT u FROM WynnExtrasUser u WHERE u.lastSeen > :cutoff")
    List<WynnExtrasUser> findActiveUsersSince(@Param("cutoff") Instant cutoff);

    /**
     * Get just the UUIDs of active users (more efficient for the badge list)
     */
    @Query("SELECT u.uuid FROM WynnExtrasUser u WHERE u.lastSeen > :cutoff")
    List<String> findActiveUuidsSince(@Param("cutoff") Instant cutoff);

    /**
     * Count active users
     */
    @Query("SELECT COUNT(u) FROM WynnExtrasUser u WHERE u.lastSeen > :cutoff")
    long countActiveUsersSince(@Param("cutoff") Instant cutoff);
}

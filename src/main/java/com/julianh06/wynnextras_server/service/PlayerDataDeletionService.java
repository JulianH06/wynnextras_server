package com.julianh06.wynnextras_server.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deletes every row that can be tied directly to a Minecraft player UUID. */
@Service
public class PlayerDataDeletionService {
    private final JdbcTemplate jdbcTemplate;

    public PlayerDataDeletionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public DeletionResult deleteAllForPlayer(String normalizedUuid) {
        String dashedUuid = normalizedUuid.substring(0, 8) + "-"
                + normalizedUuid.substring(8, 12) + "-"
                + normalizedUuid.substring(12, 16) + "-"
                + normalizedUuid.substring(16, 20) + "-"
                + normalizedUuid.substring(20);

        Set<String> usernames = findKnownUsernames(normalizedUuid, dashedUuid);
        Map<String, Integer> deleted = new LinkedHashMap<>();

        // The legacy aspect table has a foreign key to player, so it must go first.
        deleted.put("legacyAspects", deleteByUuid("aspect", "user_uuid", normalizedUuid, dashedUuid));
        deleted.put("legacyPlayers", deleteByUuid("player", "uuid", normalizedUuid, dashedUuid));
        deleted.put("personalAspects", deleteByUuid("personal_aspect", "player_uuid", normalizedUuid, dashedUuid));
        deleted.put("aspectPreferences", deleteByUuid("aspect_publication_preference", "player_uuid", normalizedUuid, dashedUuid));
        deleted.put("achievements", deleteByUuid("player_achievement", "player_uuid", normalizedUuid, dashedUuid));
        deleted.put("badges", deleteByUuid("badge_profile", "player_uuid", normalizedUuid, dashedUuid));
        deleted.put("dailyActivity", deleteByUuid("daily_user_activity", "user_uuid", normalizedUuid, dashedUuid));
        deleted.put("wynncraftSightings", deleteByUuid("wynncraft_player_sighting", "player_uuid", normalizedUuid, dashedUuid));
        deleted.put("users", deleteByUuid("wynnextras_user", "uuid", normalizedUuid, dashedUuid));

        deleted.put("raidSubmissions", deleteByUsernames("raid_lootpool_submission", usernames));
        deleted.put("lootrunSubmissions", deleteByUsernames("lootrun_lootpool_submission", usernames));
        deleted.put("gambitSubmissions", deleteByUsernames("gambit_submission", usernames));
        deleted.put("verifiedUsers", deleteVerifiedUsers(usernames));

        int totalDeleted = deleted.values().stream().mapToInt(Integer::intValue).sum();
        return new DeletionResult(normalizedUuid, List.copyOf(usernames), deleted, totalDeleted);
    }

    private Set<String> findKnownUsernames(String normalizedUuid, String dashedUuid) {
        Set<String> usernames = new LinkedHashSet<>();
        addUsernames(usernames, "SELECT username FROM wynnextras_user WHERE uuid IN (?, ?)", normalizedUuid, dashedUuid);
        addUsernames(usernames, "SELECT username FROM badge_profile WHERE player_uuid IN (?, ?)", normalizedUuid, dashedUuid);
        addUsernames(usernames, "SELECT username FROM daily_user_activity WHERE user_uuid IN (?, ?)", normalizedUuid, dashedUuid);
        addUsernames(usernames, "SELECT player_name FROM personal_aspect WHERE player_uuid IN (?, ?)", normalizedUuid, dashedUuid);
        addUsernames(usernames, "SELECT player_name FROM player_achievement WHERE player_uuid IN (?, ?)", normalizedUuid, dashedUuid);
        addUsernames(usernames, "SELECT player_name FROM player WHERE uuid IN (?, ?)", normalizedUuid, dashedUuid);
        return usernames;
    }

    private void addUsernames(Set<String> target, String sql, String normalizedUuid, String dashedUuid) {
        jdbcTemplate.queryForList(sql, String.class, normalizedUuid, dashedUuid).stream()
                .filter(name -> name != null && !name.isBlank())
                .forEach(target::add);
    }

    private int deleteByUuid(String table, String column, String normalizedUuid, String dashedUuid) {
        return jdbcTemplate.update("DELETE FROM " + table + " WHERE " + column + " IN (?, ?)", normalizedUuid, dashedUuid);
    }

    private int deleteByUsernames(String table, Set<String> usernames) {
        int deleted = 0;
        for (String username : usernames) {
            deleted += jdbcTemplate.update(
                    "DELETE FROM " + table + " WHERE LOWER(submitted_by) = LOWER(?)", username);
        }
        return deleted;
    }

    private int deleteVerifiedUsers(Set<String> usernames) {
        int deleted = 0;
        for (String username : usernames) {
            deleted += jdbcTemplate.update("DELETE FROM verified_user WHERE LOWER(username) = LOWER(?)", username);
        }
        return deleted;
    }

    public record DeletionResult(
            String playerUuid,
            List<String> usernames,
            Map<String, Integer> deleted,
            int totalDeleted
    ) {}
}

package com.julianh06.wynnextras_server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** PostgreSQL migrations that must run after Hibernate has created missing tables. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseMigrationService implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationService.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        widenAnonymousIds();
        backfillBadgeProfiles();
    }

    private void widenAnonymousIds() {
        widenAnonymousId("anonymous_user_activity");
        widenAnonymousId("anonymous_daily_activity");
    }

    private void widenAnonymousId(String tableName) {
        Integer currentLength = jdbcTemplate.queryForObject("""
                SELECT character_maximum_length
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = ?
                  AND column_name = 'anonymous_id'
                """, Integer.class, tableName);

        if (currentLength != null && currentLength < 64) {
            jdbcTemplate.execute("ALTER TABLE " + tableName
                    + " ALTER COLUMN anonymous_id TYPE varchar(64)");
            logger.info("Widened {}.anonymous_id to varchar(64)", tableName);
        }
    }

    private void backfillBadgeProfiles() {
        int inserted = jdbcTemplate.update("""
                INSERT INTO badge_profile
                    (player_uuid, username, badge_icon_id, badge_color_id, published, badge_last_seen)
                SELECT uuid, username, badge_icon_id, badge_color_id, badge_published, last_seen
                FROM wynnextras_user
                ON CONFLICT (player_uuid) DO NOTHING
                """);
        logger.info("Backfilled {} missing badge profiles", inserted);
    }
}

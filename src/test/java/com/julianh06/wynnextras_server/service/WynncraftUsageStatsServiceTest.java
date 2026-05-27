package com.julianh06.wynnextras_server.service;

import com.julianh06.wynnextras_server.entity.DailyUserActivity;
import com.julianh06.wynnextras_server.entity.WynnExtrasUser;
import com.julianh06.wynnextras_server.entity.WynncraftUsageSnapshot;
import com.julianh06.wynnextras_server.repository.DailyUserActivityRepository;
import com.julianh06.wynnextras_server.repository.WynnExtrasUserRepository;
import com.julianh06.wynnextras_server.repository.WynncraftPlayerSightingRepository;
import com.julianh06.wynnextras_server.repository.WynncraftUsageSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class WynncraftUsageStatsServiceTest {
    private static final String USER_A = "11111111111111111111111111111111";
    private static final String USER_B = "22222222222222222222222222222222";
    private static final String USER_C = "33333333333333333333333333333333";
    private static final String OLD_USER = "99999999999999999999999999999999";

    @Autowired
    private WynncraftPlayerSightingRepository playerSightingRepository;

    @Autowired
    private WynncraftUsageSnapshotRepository usageSnapshotRepository;

    @Autowired
    private WynnExtrasUserRepository wynnExtrasUserRepository;

    @Autowired
    private DailyUserActivityRepository dailyUserActivityRepository;

    private WynncraftUsageStatsService service;

    @BeforeEach
    void setUp() {
        service = new WynncraftUsageStatsService(
                null,
                playerSightingRepository,
                usageSnapshotRepository);
    }

    @Test
    void aggregatesDistinctVisiblePlayersAndDailyActiveWynnExtrasUsersForSnapshotDay() {
        LocalDate snapshotDate = LocalDate.parse("2026-05-27");
        Instant capturedAt = Instant.parse("2026-05-27T12:00:00Z");
        service.storeOnlinePlayerSample(Instant.parse("2026-05-26T23:00:00Z"), Set.of(OLD_USER));
        service.storeOnlinePlayerSample(Instant.parse("2026-05-27T00:00:00Z"), Set.of(USER_A, USER_B));
        service.storeOnlinePlayerSample(capturedAt.minusSeconds(3600), Set.of(USER_A, USER_C));
        saveWynnExtrasUser(USER_A);
        saveWynnExtrasUser(USER_C);
        saveDailyActivity(snapshotDate, USER_A);

        WynncraftUsageSnapshot snapshot = service.captureDailyUsageSnapshot(snapshotDate, capturedAt);

        assertThat(snapshot.getUniquePlayers()).isEqualTo(3);
        assertThat(snapshot.getWynnExtrasUsers()).isEqualTo(1);
        assertThat(snapshot.getSampleCount()).isEqualTo(2);
        assertThat(snapshot.getUsagePercent()).isCloseTo(33.333, within(0.01));
        assertThat(snapshot.getErrorMessage()).isNull();
    }

    @Test
    void updatesExistingSnapshotForSameDate() {
        Instant capturedAt = Instant.parse("2026-05-27T01:00:00Z");
        LocalDate snapshotDate = LocalDate.parse("2026-05-27");
        service.storeOnlinePlayerSample(capturedAt.minusSeconds(3600), Set.of(USER_A));

        WynncraftUsageSnapshot first = service.captureDailyUsageSnapshot(snapshotDate, capturedAt);

        service.storeOnlinePlayerSample(capturedAt.minusSeconds(1800), Set.of(USER_A, USER_B));
        saveWynnExtrasUser(USER_A);
        saveDailyActivity(snapshotDate, USER_A);
        WynncraftUsageSnapshot second = service.captureDailyUsageSnapshot(snapshotDate, capturedAt);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getUniquePlayers()).isEqualTo(2);
        assertThat(second.getWynnExtrasUsers()).isEqualTo(1);
        assertThat(usageSnapshotRepository.count()).isEqualTo(1);
    }

    @Test
    void prunesRawSightingsOlderThanFourteenDaysAfterSuccessfulSnapshot() {
        Instant capturedAt = Instant.parse("2026-05-27T01:00:00Z");
        service.storeOnlinePlayerSample(capturedAt.minusSeconds(15 * 86400L), Set.of(OLD_USER));
        service.storeOnlinePlayerSample(capturedAt.minusSeconds(3600), Set.of(USER_A));

        service.captureDailyUsageSnapshot(LocalDate.parse("2026-05-27"), capturedAt);

        assertThat(playerSightingRepository.count()).isEqualTo(1);
    }

    @Test
    void emptyWindowProducesZeroPercentSnapshot() {
        Instant capturedAt = Instant.parse("2026-05-27T01:00:00Z");

        WynncraftUsageSnapshot snapshot = service.captureDailyUsageSnapshot(LocalDate.parse("2026-05-27"), capturedAt);

        assertThat(snapshot.getUniquePlayers()).isZero();
        assertThat(snapshot.getWynnExtrasUsers()).isZero();
        assertThat(snapshot.getSampleCount()).isZero();
        assertThat(snapshot.getUsagePercent()).isZero();
        assertThat(snapshot.getErrorMessage()).isNull();
    }

    private void saveWynnExtrasUser(String uuid) {
        WynnExtrasUser user = new WynnExtrasUser(uuid, "user" + uuid.substring(0, 4), "1.0.0");
        user.setCreatedAt(Instant.parse("2026-05-01T00:00:00Z"));
        user.setLastSeen(Instant.parse("2026-05-27T00:00:00Z"));
        wynnExtrasUserRepository.save(user);
    }

    private void saveDailyActivity(LocalDate activityDate, String uuid) {
        dailyUserActivityRepository.save(new DailyUserActivity(
                activityDate,
                uuid,
                "user" + uuid.substring(0, 4),
                "1.0.0",
                activityDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)));
    }

    private static org.assertj.core.data.Offset<Double> within(double offset) {
        return org.assertj.core.data.Offset.offset(offset);
    }
}

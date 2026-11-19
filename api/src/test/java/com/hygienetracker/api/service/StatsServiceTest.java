package com.hygienetracker.api.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.hygienetracker.api.repository.ActivityLogRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests with a mocked repository and a pinned clock: no Spring context, no database. */
class StatsServiceTest {

    private static final ZoneId PACIFIC = ZoneId.of("America/Los_Angeles");

    /** 2026-11-13 at 05:00 UTC is still 2026-11-12 (9 PM) in Pacific time. */
    private static final Clock LATE_EVENING_PACIFIC = Clock.fixed(Instant.parse("2026-11-13T05:00:00Z"), PACIFIC);
    private static final LocalDate PACIFIC_TODAY = LocalDate.of(2026, 11, 12);

    private final ActivityLogRepository activityLogs = mock(ActivityLogRepository.class);
    private final StatsService service = new StatsService(activityLogs, new ScoreCalculator(), LATE_EVENING_PACIFIC);

    @Test
    void readsOnlyTheSignedInUsersShowers() {
        when(activityLogs.findShowerDatesByUserId(7L)).thenReturn(List.of());

        service.statsFor(7L);

        verify(activityLogs).findShowerDatesByUserId(7L);
    }

    @Test
    void lastShowerThreeDaysAgoIsThreeDaysAndSixPointsLost() {
        when(activityLogs.findShowerDatesByUserId(7L)).thenReturn(List.of(PACIFIC_TODAY.minusDays(3)));

        Stats stats = service.statsFor(7L);

        assertThat(stats.daysSinceLastShower()).isEqualTo(3);
        assertThat(stats.pointsLostToMissedDays()).isEqualTo(6);
        assertThat(stats.streak()).isZero();
        assertThat(stats.score()).isEqualTo(10 - 6);
    }

    @Test
    void neverShoweredIsZerosAndNullDaysSinceLastShower() {
        when(activityLogs.findShowerDatesByUserId(7L)).thenReturn(List.of());

        Stats stats = service.statsFor(7L);

        assertThat(stats).isEqualTo(new Stats(0, 0, null, 0));
    }

    @Test
    void todayIsTheDateInTheClocksTimeZoneNotUtc() {
        // In UTC it is already 2026-11-13, so if the service used UTC, a shower "today" in Pacific
        // time would show as 1 day since the last shower instead of 0.
        when(activityLogs.findShowerDatesByUserId(7L)).thenReturn(List.of(PACIFIC_TODAY));

        Stats stats = service.statsFor(7L);

        assertThat(stats.daysSinceLastShower()).isZero();
        assertThat(stats.streak()).isEqualTo(1);
    }
}

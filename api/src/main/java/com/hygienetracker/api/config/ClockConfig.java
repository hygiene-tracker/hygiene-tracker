package com.hygienetracker.api.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The clock that decides what "today" is for streaks and scores. Render runs in UTC, but our users
 * are on campus, so "today" is the date in {@code app.time-zone} (default America/Los_Angeles).
 * Services take this {@link Clock} instead of calling {@code LocalDate.now()}, so tests can pin it.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.time-zone:America/Los_Angeles}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}

package com.hygienetracker.api.service;

import java.time.Clock;
import java.time.LocalDate;

import com.hygienetracker.api.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds a user's stats (PRD-5) from their own shower history and the scoring rules. */
@Service
public class StatsService {

    private final ActivityLogRepository activityLogs;
    private final ScoreCalculator calculator;
    private final Clock clock;

    public StatsService(ActivityLogRepository activityLogs, ScoreCalculator calculator, Clock clock) {
        this.activityLogs = activityLogs;
        this.calculator = calculator;
        this.clock = clock;
    }

    /**
     * Calculates the stats for one user from only that user's showers.
     *
     * @param userId the signed-in user's id (the JWT subject)
     * @return the user's streak, score, days since their last shower and points lost since it
     */
    @Transactional(readOnly = true)
    public Stats statsFor(long userId) {
        ScoreSummary summary = calculator.calculate(
                activityLogs.findShowerDatesByUserId(userId), LocalDate.now(clock));
        return new Stats(
                summary.streak(),
                summary.score(),
                summary.daysSinceLastShower(),
                ScoreCalculator.pointsLostSinceLastShower(summary.daysSinceLastShower()));
    }
}

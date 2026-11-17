package com.hygienetracker.api.service;

/**
 * A user's shower stats, as calculated by {@link ScoreCalculator}.
 *
 * @param streak              consecutive days with a shower, ending today (or yesterday if the user
 *                            has not showered yet today)
 * @param score               points earned minus points lost over the scoring window; can be negative
 * @param pointsLost          points lost to missed days over the scoring window (zero or positive)
 * @param daysSinceLastShower whole days since the most recent shower (0 if today), or {@code null}
 *                            if the user has never showered
 */
public record ScoreSummary(int streak, int score, int pointsLost, Integer daysSinceLastShower) {

    /** The stats of a user who has never showered. */
    static final ScoreSummary EMPTY = new ScoreSummary(0, 0, 0, null);
}

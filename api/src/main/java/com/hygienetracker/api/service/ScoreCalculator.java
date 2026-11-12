package com.hygienetracker.api.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

/**
 * Calculates a user's streak and score from their shower history (PRD-5). Pure logic: no database,
 * no clock. Callers pass in the shower days and {@code today}, so results are easy to test.
 *
 * <p>Rules (recorded in {@code docs/prd.md} under PRD-5):
 * <ul>
 *   <li>Each day's 1st shower earns {@value #FIRST_SHOWER_POINTS} points, the 2nd
 *       {@value #SECOND_SHOWER_POINTS}, and the 3rd and later earn nothing.</li>
 *   <li>A day with no shower is a missed day, except today (the user can still shower) and days
 *       before the user's first shower. The k-th missed day in a row costs
 *       {@value #MISSED_DAY_PENALTY_STEP}&nbsp;&times;&nbsp;k points (2, 4, 6, ...), with no cap.</li>
 *   <li>The score covers the last {@value #WINDOW_DAYS} days including today, the same window as
 *       the leaderboard, and can go below zero. A run of missed days that began before the window
 *       keeps counting up from where it began.</li>
 *   <li>The streak and days since the last shower use the full history.</li>
 * </ul>
 */
@Service
public class ScoreCalculator {

    static final int WINDOW_DAYS = 30;
    static final int FIRST_SHOWER_POINTS = 10;
    static final int SECOND_SHOWER_POINTS = 5;
    static final int MISSED_DAY_PENALTY_STEP = 2;

    /**
     * Calculates the stats for one user.
     *
     * @param showerDays the local date of each shower, one entry per shower, in any order
     * @param today      the user's current local date
     * @return the user's streak, score, points lost and days since their last shower
     * @throws IllegalArgumentException if a shower is dated after {@code today}
     */
    public ScoreSummary calculate(Collection<LocalDate> showerDays, LocalDate today) {
        if (showerDays.isEmpty()) {
            return ScoreSummary.EMPTY;
        }

        TreeMap<LocalDate, Integer> showersPerDay = new TreeMap<>();
        for (LocalDate day : showerDays) {
            showersPerDay.merge(day, 1, Integer::sum);
        }

        LocalDate firstShower = showersPerDay.firstKey();
        LocalDate lastShower = showersPerDay.lastKey();
        if (lastShower.isAfter(today)) {
            throw new IllegalArgumentException(
                    "Shower on " + lastShower + " is after today (" + today + ")");
        }

        LocalDate windowStart = today.minusDays(WINDOW_DAYS - 1);
        int earned = pointsEarned(showersPerDay.tailMap(windowStart, true));
        int lost = pointsLost(showersPerDay, firstShower, windowStart, today);
        int daysSinceLastShower = (int) ChronoUnit.DAYS.between(lastShower, today);

        return new ScoreSummary(streak(showersPerDay, today), earned - lost, lost, daysSinceLastShower);
    }

    /** Points for one day's showers: 10 for the first, 5 for the second, nothing after that. */
    static int pointsForDay(int showers) {
        if (showers >= 2) {
            return FIRST_SHOWER_POINTS + SECOND_SHOWER_POINTS;
        }
        return showers == 1 ? FIRST_SHOWER_POINTS : 0;
    }

    private static int pointsEarned(Map<LocalDate, Integer> showersInWindow) {
        return showersInWindow.values().stream().mapToInt(ScoreCalculator::pointsForDay).sum();
    }

    /**
     * Walks every day from the first shower up to yesterday, counting each run of missed days, and
     * adds up the penalties for the missed days inside the window.
     */
    private static int pointsLost(Map<LocalDate, Integer> showersPerDay, LocalDate firstShower,
                                  LocalDate windowStart, LocalDate today) {
        int lost = 0;
        int missedInARow = 0;
        for (LocalDate day = firstShower; day.isBefore(today); day = day.plusDays(1)) {
            if (showersPerDay.containsKey(day)) {
                missedInARow = 0;
                continue;
            }
            missedInARow++;
            if (!day.isBefore(windowStart)) {
                lost += MISSED_DAY_PENALTY_STEP * missedInARow;
            }
        }
        return lost;
    }

    /** Consecutive shower days ending today, or ending yesterday if there is no shower yet today. */
    private static int streak(Map<LocalDate, Integer> showersPerDay, LocalDate today) {
        LocalDate day = showersPerDay.containsKey(today) ? today : today.minusDays(1);
        int streak = 0;
        while (showersPerDay.containsKey(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }
}

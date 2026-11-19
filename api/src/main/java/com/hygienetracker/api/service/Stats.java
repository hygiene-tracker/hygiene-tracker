package com.hygienetracker.api.service;

/**
 * The signed-in user's stats, as returned by {@code GET /api/me/stats}. Field names match the
 * {@code Stats} schema in {@code docs/openapi.yaml}.
 *
 * @param streak                 consecutive days with a shower, ending today or yesterday
 * @param score                  score over the last 30 days; can be negative
 * @param daysSinceLastShower    whole days since the last shower, or {@code null} if never showered
 * @param pointsLostToMissedDays points lost to missed days since the last shower
 */
public record Stats(int streak, int score, Integer daysSinceLastShower, int pointsLostToMissedDays) {
}

package com.hygienetracker.api.service;

import java.util.Comparator;

import com.hygienetracker.api.entity.User;
import com.hygienetracker.api.repository.UserRepository;
import org.springframework.stereotype.Service;

/** Retrieves the lowest-scoring active user for the monthly leaderboard. */
@Service
public class LeaderboardService {

    private final UserRepository users;
    private final StatsService statsService;

    public LeaderboardService(UserRepository users, StatsService statsService) {
        this.users = users;
        this.statsService = statsService;
    }

    /**
     * Returns the user with the lowest score. Scores are allowed to be negative.
     *
     * @throws IllegalStateException when there are no users to rank yet
     */
    public User loserOfMonth() {
        return users.findAll().stream()
                .min(Comparator.comparingInt(user -> statsService.statsFor(user.getId()).score()))
                .orElseThrow(() -> new IllegalStateException("Cannot retrieve loser of the month: no users available"));
    }
}

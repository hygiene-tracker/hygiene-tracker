package com.hygienetracker.api.service;

import java.util.List;

import com.hygienetracker.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private UserRepository users;

    @Mock
    private StatsService statsService;

    @InjectMocks
    private LeaderboardService leaderboardService;

    @Test
    void loserOfMonthThrowsExpectedExceptionWhenNoUsersAreAvailable() {
        given(users.findAll()).willReturn(List.of());

        assertThatThrownBy(() -> leaderboardService.loserOfMonth())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot retrieve loser of the month: no users available");
    }
}
